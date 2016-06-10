package com.peppermint.app.ui.base;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Nuno Luz on 04-02-2016.
 *
 * Utility class to request and enforce a set of permissions in Android 6.
 *
 */
public class PermissionsPolicyEnforcer {

    public static final int PERMISSION_REQUEST_CODE = 113;

    protected class RequiredPermission {
        String permission;
        Set<String> requiredFeatures;
        boolean optional;
        RequiredPermission(String permission, Set<String> requiredFeatures, boolean optional) {
            this.permission = permission;
            this.requiredFeatures = requiredFeatures;
            this.optional = optional;
        }
    }

    private List<RequiredPermission> mPermissions;
    private List<RequiredPermission> mPermissionsToAsk;

    public PermissionsPolicyEnforcer() {
        mPermissions = new ArrayList<>();
    }

    public PermissionsPolicyEnforcer(String... permissions) {
        this();
        if(permissions != null) {
            for(String permission : permissions) {
                mPermissions.add(new RequiredPermission(permission, null, false));
            }
        }
    }

    public void addPermission(String permission, boolean optional) {
        addPermission(permission, optional, (String[]) null);
    }

    public void addPermission(String permission, boolean optional, String... requiredFeatures) {
        HashSet<String> set = null;
        if(requiredFeatures != null) {
            set = new HashSet<>();
            Collections.addAll(set, requiredFeatures);
        }

        mPermissions.add(new RequiredPermission(permission, set, optional));
    }

    public boolean requestPermissions(Activity activityRequestingPermissions) {
        List<String> tmpPermissionsToAsk = getPermissionsToAsk(activityRequestingPermissions);

        if (tmpPermissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(activityRequestingPermissions,
                    tmpPermissionsToAsk.toArray(new String[tmpPermissionsToAsk.size()]),
                    PERMISSION_REQUEST_CODE);
            return true;
        }

        return false;
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_CODE && mPermissionsToAsk != null) {
            if (grantResults.length == mPermissionsToAsk.size()) {
                boolean permissionsGranted = true;
                for(int i=0; i<grantResults.length && permissionsGranted; i++) {
                    if(!mPermissionsToAsk.get(i).optional) {
                        permissionsGranted = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                }
                return permissionsGranted;
            }

            return false;
        }

        return true;
    }

    public List<String> getPermissionsToAsk(Context context) {
        mPermissionsToAsk = new ArrayList<>();
        List<String> tmpPermissionsToAsk = new ArrayList<>();

        for(RequiredPermission permission : mPermissions) {

            boolean requirementsOk = true;
            if(permission.requiredFeatures != null) {
                Iterator<String> featureIt = permission.requiredFeatures.iterator();
                while(featureIt.hasNext() && requirementsOk) {
                    String feature = featureIt.next();
                    requirementsOk = context.getPackageManager().hasSystemFeature(feature);
                }
            }

            if(requirementsOk && ContextCompat.checkSelfPermission(context, permission.permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToAsk.add(permission);
                tmpPermissionsToAsk.add(permission.permission);
            }
        }

        return tmpPermissionsToAsk;
    }
}
