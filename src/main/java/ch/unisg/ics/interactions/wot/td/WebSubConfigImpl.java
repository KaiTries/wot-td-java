package ch.unisg.ics.interactions.wot.td;

/**
 * Implementation of the WebSubConfig interface.
 * This class represents the configuration for WebSub,
 * a protocol for distributed publish-subscribe communication on the web.
 * It provides methods to retrieve the configuration settings for WebSub,
 * such as whether it is enabled and the WebSub hub URI.
 */
public class WebSubConfigImpl {

  private final boolean enabled;
  private final String webSubHubUri;

  /**
   * Constructs a new WebSubConfigImpl object with the specified configuration
   * and HTTP interface configuration.
   *
   */
  public WebSubConfigImpl(final boolean enabled, final String webSubHubUri) {
    this.enabled = enabled;
    this.webSubHubUri =webSubHubUri;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public String getWebSubHubUri() {
    return this.webSubHubUri;
  }
}
