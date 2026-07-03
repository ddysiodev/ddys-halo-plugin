package io.ddys.halo.ddysopen.config;

public class DisplaySettings {
    private String pageTitle = "DDYS";
    private String defaultLayout = "grid";
    private String theme = "auto";
    private Integer pageSize = 12;
    private Boolean showAttribution = true;
    private Boolean openInNewTab = true;

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getDefaultLayout() {
        return defaultLayout;
    }

    public void setDefaultLayout(String defaultLayout) {
        this.defaultLayout = defaultLayout;
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Boolean getShowAttribution() {
        return showAttribution;
    }

    public void setShowAttribution(Boolean showAttribution) {
        this.showAttribution = showAttribution;
    }

    public Boolean getOpenInNewTab() {
        return openInNewTab;
    }

    public void setOpenInNewTab(Boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
    }
}

