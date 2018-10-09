/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.navigation.ui;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.v7.widget.Toolbar;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;

import java.lang.ref.WeakReference;

/**
 * The OnNavigatedListener specifically for keeping a Toolbar updated. This handles both
 * updating the title and updating the Up Indicator, transitioning between the drawer icon and
 * up arrow as needed.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class ToolbarOnNavigatedListener extends AbstractAppBarOnNavigatedListener {
    private final WeakReference<Toolbar> mToolbarWeakReference;

    ToolbarOnNavigatedListener(
            @NonNull Toolbar toolbar, @NonNull AppBarConfiguration configuration) {
        super(toolbar.getContext(), configuration);
        mToolbarWeakReference = new WeakReference<>(toolbar);
    }

    @Override
    public void onNavigated(@NonNull NavController controller,
            @NonNull NavDestination destination) {
        Toolbar toolbar = mToolbarWeakReference.get();
        if (toolbar == null) {
            controller.removeOnNavigatedListener(this);
            return;
        }
        super.onNavigated(controller, destination);
    }

    @Override
    protected void setTitle(CharSequence title) {
        mToolbarWeakReference.get().setTitle(title);
    }

    @Override
    protected void setNavigationIcon(Drawable icon) {
        Toolbar toolbar = mToolbarWeakReference.get();
        if (toolbar != null) {
            toolbar.setNavigationIcon(icon);
        }
    }
}
