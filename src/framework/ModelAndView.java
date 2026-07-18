package framework;

import java.util.Map;

public class ModelAndView {
    String viewName;
    Map<String, Object> data;

    public ModelAndView(String viewName, Map<String, Object> data) {
        this.viewName = viewName;
        this.data = data;
    }

    public String getViewName() {
        return viewName;
    }

    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}