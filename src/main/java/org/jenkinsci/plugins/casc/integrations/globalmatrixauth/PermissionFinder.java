package org.jenkinsci.plugins.casc.integrations.globalmatrixauth;

import hudson.security.Permission;
import hudson.security.PermissionGroup;
import jenkins.model.Jenkins;

import javax.annotation.CheckForNull;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Created by mads on 2/9/18.
 */
public class PermissionFinder {

    /** For Matrix Auth - Title/Permission **/
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("(\\w+)/(\\w+)");

    /**
     * Attempt to match a given permission to what is defined in the UI.
     * TODO: Refector this away when proper permission API is in place for C-as-C
     * @param id String of the form "Title/Permission" (Look in the UI) for a particular permission
     * @return a matched permission
     */
    @CheckForNull
    public static Permission findPermission(String id) {
        List<PermissionGroup> pgs = PermissionGroup.getAll();
        Matcher m = PERMISSION_PATTERN.matcher(id);
        if(m.matches()) {
            String owner = m.group(1);
            String name = m.group(2);
            for(PermissionGroup pg : pgs) {
                if(pg.owner.equals(Permission.class)) {
                    continue;
                }
                //How do we do this properly, we want to mimic the UI as best as possible. So the logic conclusion is
                //That when you want admin to be Overall/Administer you put that in. Overall being the group title...
                //Name being the Permssion you want to set in the matrix.
                if(pg.title.toString().equals(owner)) {
                    return Permission.fromId(pg.owner.getName()+"."+name);
                }
            }
        }
        return null;
    }
}
