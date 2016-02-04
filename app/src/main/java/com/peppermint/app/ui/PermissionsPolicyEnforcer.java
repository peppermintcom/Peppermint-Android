package com.peppermint.app.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Created by Nuno Luz on 04-02-2016.
 */
public class PermissionsPolicyEnforcer {

    public static final int PERMISSION_REQUEST_CODE = 112;

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
        addPermission(permission, optional, null);
    }

    public void addPermission(String permission, boolean optional, String... requiredFeatures) {
        HashSet<String> set = null;
        if(requiredFeatures != null) {
            set = new HashSet<>();
            for (String feature : requiredFeatures) {
                set.add(feature);
            }
        }

        mPermissions.add(new RequiredPermission(permission, set, optional));
    }

    public boolean requestPermissions(Activity activityRequestingPermissions) {
        mPermissionsToAsk = new ArrayList<>();
        List<String> tmpPermissionsToAsk = new ArrayList<>();

        for(RequiredPermission permission : mPermissions) {

            boolean requirementsOk = true;
            if(permission.requiredFeatures != null) {
                Iterator<String> featureIt = permission.requiredFeatures.iterator();
                while(featureIt.hasNext() && requirementsOk) {
                    String feature = featureIt.next();
                    requirementsOk = requirementsOk && activityRequestingPermissions.getPackageManager().hasSystemFeature(feature);
                }
            }

            if(requirementsOk && ContextCompat.checkSelfPermission(activityRequestingPermissions, permission.permission) != PackageManager.PERMISSION_GRANTED) {
                mPermissionsToAsk.add(permission);
                tmpPermissionsToAsk.add(permission.permission);
            }
        }

        if (tmpPermissionsToAsk.size() > 0) {
            ActivityCompat.requestPermissions(activityRequestingPermissions,
                    tmpPermissionsToAsk.toArray(new String[tmpPermissionsToAsk.size()]),
                    PERMISSION_REQUEST_CODE);
            return true;
        }

        return false;
    }

    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length == mPermissionsToAsk.size()) {
                boolean permissionsGranted = true;
                for(int i=0; i<grantResults.length && permissionsGranted; i++) {
                    if(!mPermissionsToAsk.get(i).optional) {
                        permissionsGranted = permissionsGranted && grantResults[i] == PackageManager.PERMISSION_GRANTED;
                    }
                }
                return permissionsGranted;
            }

            return false;
        }

        return true;
    }

}
