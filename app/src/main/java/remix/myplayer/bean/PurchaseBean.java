package remix.myplayer.bean;

import java.io.Serializable;

public class PurchaseBean implements Serializable {
    private static final long serialVersionUID = -2117631704348736859L;
    private String id;
    private String logo;
    private String title;
    private String price;

    public PurchaseBean(String id, String logo, String title, String price) {
        this.id = id;
        this.logo = logo;
        this.title = title;
        this.price = price;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
