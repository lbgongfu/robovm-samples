/*
 * Copyright (C) 2014 RoboVM AB
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 *   
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 * 
 * Portions of this code is based on Apple Inc's LocateMe sample (v2.2)
 * which is copyright (C) 2008-2010 Apple Inc.
 */

package org.robovm.samples.locateme.viewcontrollers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.robovm.apple.coregraphics.CGPoint;
import org.robovm.apple.coregraphics.CGRect;
import org.robovm.apple.corelocation.CLErrorCode;
import org.robovm.apple.corelocation.CLLocation;
import org.robovm.apple.corelocation.CLLocationManager;
import org.robovm.apple.corelocation.CLLocationManagerDelegateAdapter;
import org.robovm.apple.foundation.Foundation;
import org.robovm.apple.foundation.NSDateFormatter;
import org.robovm.apple.foundation.NSDateFormatterStyle;
import org.robovm.apple.foundation.NSError;
import org.robovm.apple.foundation.NSIndexPath;
import org.robovm.apple.foundation.NSObjectProtocol;
import org.robovm.apple.uikit.NSLayoutConstraint;
import org.robovm.apple.uikit.NSLayoutFormatOptions;
import org.robovm.apple.uikit.NSTextAlignment;
import org.robovm.apple.uikit.UIActivityIndicatorView;
import org.robovm.apple.uikit.UIActivityIndicatorViewStyle;
import org.robovm.apple.uikit.UIBarButtonItem;
import org.robovm.apple.uikit.UIBarButtonItemStyle;
import org.robovm.apple.uikit.UIBarStyle;
import org.robovm.apple.uikit.UIButton;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UIControl;
import org.robovm.apple.uikit.UIControlState;
import org.robovm.apple.uikit.UIEvent;
import org.robovm.apple.uikit.UIFont;
import org.robovm.apple.uikit.UILabel;
import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UIRectEdge;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UITableView;
import org.robovm.apple.uikit.UITableViewCell;
import org.robovm.apple.uikit.UITableViewCellAccessoryType;
import org.robovm.apple.uikit.UITableViewCellSelectionStyle;
import org.robovm.apple.uikit.UITableViewCellStyle;
import org.robovm.apple.uikit.UITableViewDataSourceAdapter;
import org.robovm.apple.uikit.UITableViewDelegateAdapter;
import org.robovm.apple.uikit.UITableViewStyle;
import org.robovm.apple.uikit.UIView;
import org.robovm.apple.uikit.UIViewAutoresizing;
import org.robovm.apple.uikit.UIViewController;
import org.robovm.samples.locateme.Str;

public class TrackLocationViewController extends UIViewController {
    private CLLocationManager locationManager;
    private final List<CLLocation> locationMeasurements = new ArrayList<>();
    private final NSDateFormatter dateFormatter;
    private String stateString;

    private final LocationDetailViewController locationDetailViewController;
    private final UINavigationController setupNavigationController;
    private final SetupViewController setupViewController;
    private UITableView tableView;
    private UILabel descriptionLabel;
    private UIButton startButton;

    public TrackLocationViewController () {
        setupViewController = new SetupViewController();
        setupViewController.setDelegate(new SetupViewControllerDelegate() {
            @Override
            public void didFinishSetup (SetupViewController viewController, Map<String, Double> setupInfo) {
                finishSetup(viewController, setupInfo);
            }
        });
        setupNavigationController = new UINavigationController(setupViewController);
        setupNavigationController.getNavigationBar().setBarStyle(UIBarStyle.Black);
        locationDetailViewController = new LocationDetailViewController(UITableViewStyle.Grouped);

        dateFormatter = new NSDateFormatter();
        dateFormatter.setDateStyle(NSDateFormatterStyle.Medium);
        dateFormatter.setTimeStyle(NSDateFormatterStyle.Long);

        setTitle("Track Location");
        setEdgesForExtendedLayout(UIRectEdge.None);

        UIView view = getView();
        view.setBackgroundColor(UIColor.groupTableViewBackground());

        /*
         * The table view has two sections. The first has 1 row which displays status information. The second has a row for each
         * valid location object received from the location manager.
         */
        tableView = new UITableView(UIScreen.getMainScreen().getApplicationFrame(), UITableViewStyle.Grouped);
        tableView.setAlpha(0);
        view.addSubview(tableView);

        descriptionLabel = new UILabel();
        descriptionLabel
            .setText("This approach attempts to track changes to the location. The distance filter indicates the desired granularity of updates.");
        descriptionLabel.setFont(UIFont.getSystemFont(17));
        descriptionLabel.setTextColor(UIColor.black());
        descriptionLabel.setNumberOfLines(19);
        descriptionLabel.setTextAlignment(NSTextAlignment.Center);
        descriptionLabel.setTranslatesAutoresizingMaskIntoConstraints(false);
        view.addSubview(descriptionLabel);

        startButton = new UIButton();
        startButton.getTitleLabel().setFont(UIFont.getBoldSystemFont(15));
        startButton.setTitle("Start", UIControlState.Normal);
        startButton.setTitleColor(UIColor.black(), UIControlState.Normal);
        startButton.setTitleShadowColor(UIColor.gray(), UIControlState.Normal);
        startButton.setTranslatesAutoresizingMaskIntoConstraints(false);
        startButton.addOnTouchUpInsideListener(new UIControl.OnTouchUpInsideListener() {
            @Override
            public void onTouchUpInside (UIControl control, UIEvent event) {
                setupViewController.configure(true);
                getNavigationController().presentViewController(setupNavigationController, true, null);
            }
        });
        view.addSubview(startButton);

        // Layout
        Map<String, NSObjectProtocol> views = new HashMap<>();
        views.put("top", getTopLayoutGuide());
        views.put("desc", descriptionLabel);
        views.put("start", startButton);

        view.addConstraints(NSLayoutConstraint.create("H:|-20-[desc]-20-|", NSLayoutFormatOptions.None, null, views));
        view.addConstraints(NSLayoutConstraint.create("H:|-(<=100)-[start(>=50)]-(<=100)-|", NSLayoutFormatOptions.None, null,
            views));
        view.addConstraints(NSLayoutConstraint.create("V:[top]-20-[desc(187)]-[start]", NSLayoutFormatOptions.None, null, views));

        tableView.setDataSource(new UITableViewDataSourceAdapter() {
            @Override
            public long getNumberOfSections (UITableView tableView) {
                return (locationMeasurements.size() > 0) ? 2 : 1;
            }

            @Override
            public String getTitleForHeader (UITableView tableView, long section) {
                switch ((int)section) {
                case 0:
                    return Str.getLocalizedString("Status");
                default:
                    return Str.getLocalizedString("All Measurements");
                }
            }

            @Override
            public long getNumberOfRowsInSection (UITableView tableView, long section) {
                switch ((int)section) {
                case 0:
                    return 1;
                default:
                    return locationMeasurements.size();
                }
            }

            @Override
            public UITableViewCell getCellForRow (UITableView tableView, NSIndexPath indexPath) {
                UITableViewCell cell;
                switch ((int)indexPath.getSection()) {
                case 0:
                    /*
                     * The cell for the status row uses the cell style "UITableViewCellStyleValue1", which has a label on the left
                     * side of the cell with left-aligned and black text; on the right side is a label that has smaller blue text
                     * and is right-aligned. An activity indicator has been added to the cell and is animated while the location
                     * manager is updating. The cell's text label displays the current state of the manager.
                     */
                    final String StatusCellID = "StatusCellID";
                    final int StatusCellActivityIndicatorTag = 2;
                    UIActivityIndicatorView activityIndicator = null;
                    cell = tableView.dequeueReusableCell(StatusCellID);
                    if (cell == null) {
                        cell = new UITableViewCell(UITableViewCellStyle.Value1, StatusCellID);
                        cell.setSelectionStyle(UITableViewCellSelectionStyle.None);
                        activityIndicator = new UIActivityIndicatorView(UIActivityIndicatorViewStyle.Gray);
                        CGRect frame = activityIndicator.getFrame();
                        frame.setOrigin(new CGPoint(290, 12));
                        activityIndicator.setFrame(frame);
                        activityIndicator.setAutoresizingMask(UIViewAutoresizing.FlexibleLeftMargin);
                        activityIndicator.setTag(StatusCellActivityIndicatorTag);
                        cell.getContentView().addSubview(activityIndicator);
                    } else {
                        activityIndicator = (UIActivityIndicatorView)cell.getContentView().getViewWithTag(
                            StatusCellActivityIndicatorTag);
                    }

                    cell.getTextLabel().setText(stateString);
                    if (stateString != null && stateString.equals(Str.getLocalizedString("Tracking"))) {
                        if (!activityIndicator.isAnimating()) activityIndicator.startAnimating();
                    } else {
                        if (activityIndicator.isAnimating()) activityIndicator.stopAnimating();
                    }
                    return cell;
                default:
                    /*
                     * The cells for the location rows use the cell style "UITableViewCellStyleSubtitle", which has a left-aligned
                     * label across the top and a left-aligned label below it in smaller gray text. The text label shows the
                     * coordinates for the location and the detail text label shows its timestamp.
                     */
                    final String OtherMeasurementsCellID = "OtherMeasurementsCellID";
                    cell = tableView.dequeueReusableCell(OtherMeasurementsCellID);
                    if (cell == null) {
                        cell = new UITableViewCell(UITableViewCellStyle.Subtitle, OtherMeasurementsCellID);
                        cell.setAccessoryType(UITableViewCellAccessoryType.DisclosureIndicator);
                    }
                    CLLocation location = locationMeasurements.get((int)indexPath.getRow());
                    cell.getTextLabel().setText(Str.getLocalizedCoordinateString(location));
                    cell.getDetailTextLabel().setText(dateFormatter.format(location.getTimestamp()));
                    return cell;
                }
            }
        });
        tableView.setDelegate(new UITableViewDelegateAdapter() {
            /** Delegate method invoked before the user selects a row. In this sample, we use it to prevent selection in the first
             * section of the table view. */
            @Override
            public NSIndexPath willSelectRow (UITableView tableView, NSIndexPath indexPath) {
                return (indexPath.getSection() == 0) ? null : indexPath;
            }

            /** Delegate method invoked after the user selects a row. Selecting a row containing a location object will navigate to
             * a new view controller displaying details about that location. */
            @Override
            public void didSelectRow (UITableView tableView, NSIndexPath indexPath) {
                tableView.deselectRow(indexPath, true);
                CLLocation location = locationMeasurements.get((int)indexPath.getRow());
                locationDetailViewController.setLocation(location);
                getNavigationController().pushViewController(locationDetailViewController, true);
            }
        });
    }

    /** The reset method allows the user to repeatedly test the location functionality. In addition to discarding all of the
     * location measurements from the previous "run", it animates a transition in the user interface between the table which
     * displays location data and the start button and description label presented at launch. */
    private void reset () {
        locationMeasurements.clear();
        UIView.beginAnimations("Reset", null);
        UIView.setAnimationDurationInSeconds(0.6);
        startButton.setAlpha(1);
        descriptionLabel.setAlpha(1);
        tableView.setAlpha(0);
        getNavigationItem().setLeftBarButtonItem(null, true);
        UIView.commitAnimations();
    }

    /** This method is invoked when the user hits "Done" in the setup view controller. The options chosen by the user are passed in
     * as a map. The keys for this map are declared in SetupViewController. */
    private void finishSetup (SetupViewController controller, Map<String, Double> setupInfo) {
        startButton.setAlpha(0);
        descriptionLabel.setAlpha(0);
        tableView.setAlpha(1);
        // Create the manager object
        locationManager = new CLLocationManager();
        if (Foundation.getMajorSystemVersion() >= 8) {
            locationManager.requestAlwaysAuthorization();
        }
        locationManager.setDelegate(new CLLocationManagerDelegateAdapter() {
            /** We want to get and store a location measurement that meets the desired accuracy. For this example, we are going to
             * use horizontal accuracy as the deciding factor. In other cases, you may wish to use vertical accuracy, or both
             * together. */
            @Override
            public void didUpdateToLocation (CLLocationManager manager, CLLocation newLocation, CLLocation oldLocation) {
                // test that the horizontal accuracy does not indicate an invalid measurement
                if (newLocation.getHorizontalAccuracy() < 0) return;
                // test the age of the location measurement to determine if the measurement is cached
                // in most cases you will not want to rely on cached measurements
                double locationAge = -newLocation.getTimestamp().getTimeIntervalSinceNow();
                if (locationAge > 5.0) return;
                // store all of the measurements, just so we can see what kind of data we might receive
                locationMeasurements.add(newLocation);
                // update the display with the new location data
                tableView.reloadData();
            }

            @Override
            public void didFail (CLLocationManager manager, NSError error) {
                // The location "unknown" error simply means the manager is currently unable to get the location.
                if (error.getErrorCode() != CLErrorCode.LocationUnknown) {
                    stopUpdatingLocation(Str.getLocalizedString("Error"));
                }
            }
        });
        // This is the most important property to set for the manager. It ultimately determines how the manager will
        // attempt to acquire location and thus, the amount of power that will be consumed.
        locationManager.setDesiredAccuracy(setupInfo.get(SetupViewController.SETUP_INFO_KEY_ACCURACY));
        // When "tracking" the user, the distance filter can be used to control the frequency with which location measurements
        // are delivered by the manager. If the change in distance is less than the filter, a location will not be delivered.
        locationManager.setDistanceFilter(setupInfo.get(SetupViewController.SETUP_INFO_KEY_DISTANCE_FILTER));
        // Once configured, the location manager must be "started".
        locationManager.startUpdatingLocation();

        stateString = Str.getLocalizedString("Tracking");
        tableView.reloadData();
        UIBarButtonItem resetItem = new UIBarButtonItem(Str.getLocalizedString("Reset"), UIBarButtonItemStyle.Plain,
            new UIBarButtonItem.OnClickListener() {
                @Override
                public void onClick (UIBarButtonItem barButtonItem) {
                    reset();
                }
            });
        getNavigationItem().setLeftBarButtonItem(resetItem, true);
    }

    private void stopUpdatingLocation (String state) {
        stateString = state;
        tableView.reloadData();
        locationManager.stopUpdatingLocation();
    }
}
