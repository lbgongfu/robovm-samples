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
 * Portions of this code is based on Apple Inc's QuickContacts sample (v1.0)
 * which is copyright (C) 2008-2013 Apple Inc.
 * 
 * The view controller creates a few bulb views which host the custom layer subclass.
 */

package org.robovm.samples.customanimateproperty;

import org.robovm.apple.foundation.NSAutoreleasePool;
import org.robovm.apple.foundation.NSDictionary;
import org.robovm.apple.foundation.NSString;
import org.robovm.apple.uikit.UIApplication;
import org.robovm.apple.uikit.UIApplicationDelegateAdapter;
import org.robovm.apple.uikit.UIColor;
import org.robovm.apple.uikit.UINavigationController;
import org.robovm.apple.uikit.UIScreen;
import org.robovm.apple.uikit.UIWindow;
import org.robovm.samples.customanimateproperty.viewcontrollers.ViewController;

/**
 * Main class
 */
public class CustomAnimatableProperty extends UIApplicationDelegateAdapter {

    private UIWindow window = null;

    @Override
    public boolean didFinishLaunching(UIApplication application,
            NSDictionary<NSString, ?> launchOptions) {

        window = new UIWindow(UIScreen.getMainScreen().getBounds());
        window.setBackgroundColor(UIColor.colorLightGray());
        UINavigationController navigationController = new UINavigationController(
                application.addStrongRef(new ViewController()));
        window.setRootViewController(navigationController);
        window.makeKeyAndVisible();

        // Ties UIWindow instance together with UIApplication object on the
        // Objective C side of things
        // Basically meaning that it wont be GC:ed on the java side until it is
        // on the Objective C side
        application.addStrongRef(window);

        return true;
    }

    public static void main(String[] args) {
        NSAutoreleasePool pool = new NSAutoreleasePool();
        UIApplication.main(args, null, CustomAnimatableProperty.class);
        pool.close();
    }
    

}
