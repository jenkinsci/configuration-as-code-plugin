package io.jenkins.plugins.casc.yaml;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Api;
import hudson.model.UnprotectedRootAction;
import hudson.util.HttpResponses;
import javax.annotation.CheckForNull;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import org.jenkinsci.Symbol;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.HttpResponse;
import org.kohsuke.stapler.export.ExportedBean;

@Extension
@Symbol("cascMergeStrategy")
@ExportedBean
public class MergeStrategyAction implements UnprotectedRootAction {

    public Api getApi() {
        return new Api(this);
    }

    @CheckForNull
    @Override
    public String getIconFileName() {
        return null;
    }

    @CheckForNull
    @Override
    public String getDisplayName() {
        return "cascMergeStrategy";
    }

    @CheckForNull
    @Override
    public String getUrlName() {
        return "cascMergeStrategy";
    }

    @Restricted(NoExternalUse.class)
    public HttpResponse doIndex() {
        JSONArray array = new JSONArray();

        ExtensionList<MergeStrategy> mergeStrategyList = Jenkins.get()
            .getExtensionList(MergeStrategy.class);
        array.addAll(mergeStrategyList);

        return HttpResponses.okJSON(array);
    }
}
