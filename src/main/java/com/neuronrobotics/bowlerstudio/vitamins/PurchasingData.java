package com.neuronrobotics.bowlerstudio.vitamins;

import java.util.HashMap;

public class PurchasingData {
  private HashMap<String, Double> variantParameters;
  private HashMap<Integer, Double> pricsUSD;
  private String urlAPI;
  private String db;
  private String serverType;
  private String cartURL;

  public PurchasingData() {
    variantParameters = new HashMap<>();
    variantParameters.put("Bolt Length", 10.0);
    pricsUSD = new HashMap<>();
    pricsUSD.put(1, 0.02);
    urlAPI = "http://localhost:8069/";
    db = "testdatabse";
    serverType = "odoo";
    cartURL = "http://localhost:8069/shop/product/m3-socket-cap-screw-73";
  }

  public HashMap<String, Double> getVariantParameters() {
    return variantParameters;
  }

  public void setVariantParameters(HashMap<String, Double> variantParameters) {
    this.variantParameters = variantParameters;
  }

  public double getPricsUSD(int qty) {
    return pricsUSD.get(qty);
  }

  public void setPricsUSD(int qty, double pricsUSD) {
    this.pricsUSD.put(qty, pricsUSD);
  }

  public String getAPIUrl() {
    return urlAPI;
  }

  public void setAPIUrl(String url) {
    this.urlAPI = url;
  }

  public String getDatabase() {
    return db;
  }

  public void setDatabase(String db) {
    this.db = db;
  }

  public String getServerType() {
    return serverType;
  }

  public void setServerType(String serverType) {
    this.serverType = serverType;
  }

  public String getCartUrl() {
    return cartURL;
  }

  public void setCartUrl(String cartURL) {
    this.cartURL = cartURL;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder("\n");
    s.append("urlAPI ").append(urlAPI).append("\n");
    s.append("db ").append(db).append("\n");
    s.append("serverType ").append(serverType).append("\n");
    s.append("cartURL ").append(cartURL).append("\n");

    for (String key : variantParameters.keySet()) {
      s.append("variable ").append(key).append(" to ").append(variantParameters.get(key)).append
          ("\n");
    }

    for (Integer key : pricsUSD.keySet()) {
      s.append("Price at ").append(key).append(" = ").append(pricsUSD.get(key)).append("\n");
    }

    return s.toString();
  }

}
