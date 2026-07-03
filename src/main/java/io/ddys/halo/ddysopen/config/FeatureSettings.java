package io.ddys.halo.ddysopen.config;

public class FeatureSettings {
    private Boolean enableShortcodes = true;
    private Boolean enableThemeCss = true;
    private Boolean enablePages = true;
    private Boolean enablePublicApi = true;
    private Boolean enableRequestForm = false;

    public Boolean getEnableShortcodes() {
        return enableShortcodes;
    }

    public void setEnableShortcodes(Boolean enableShortcodes) {
        this.enableShortcodes = enableShortcodes;
    }

    public Boolean getEnableThemeCss() {
        return enableThemeCss;
    }

    public void setEnableThemeCss(Boolean enableThemeCss) {
        this.enableThemeCss = enableThemeCss;
    }

    public Boolean getEnablePages() {
        return enablePages;
    }

    public void setEnablePages(Boolean enablePages) {
        this.enablePages = enablePages;
    }

    public Boolean getEnablePublicApi() {
        return enablePublicApi;
    }

    public void setEnablePublicApi(Boolean enablePublicApi) {
        this.enablePublicApi = enablePublicApi;
    }

    public Boolean getEnableRequestForm() {
        return enableRequestForm;
    }

    public void setEnableRequestForm(Boolean enableRequestForm) {
        this.enableRequestForm = enableRequestForm;
    }
}

