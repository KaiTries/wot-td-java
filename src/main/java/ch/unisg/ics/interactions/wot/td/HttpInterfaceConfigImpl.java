package ch.unisg.ics.interactions.wot.td;

/**
 * Implementation of the HttpInterfaceConfig interface
 * that provides configuration for an HTTP interface.
 */
@SuppressWarnings("PMD.AvoidUsingHardCodedIP")
public class HttpInterfaceConfigImpl {

  private final String host;
  private final String baseUri;
  private final String baseUriTrailingSlash;
  private final int port;

  /**
   * Constructs a new HttpInterfaceConfigImpl object with the specified configuration.
   *
   */
  public HttpInterfaceConfigImpl(final String host, final int port, final String baseUri) {
    this.host = host;
    this.port = port;
    this.baseUri = baseUri;
    this.baseUriTrailingSlash = baseUri + "/";
  }

  public String getHost() {
    return this.host;
  }

  public int getPort() {
    return this.port;
  }

  public String getBaseUriTrailingSlash() {
    return this.baseUriTrailingSlash;
  }

  public String getBaseUri() {
    return this.baseUri;
  }

  public String getWorkspacesUriTrailingSlash() {
    return this.baseUriTrailingSlash + "workspaces/";
  }

  public String getWorkspacesUri() {
    return this.baseUriTrailingSlash + "workspaces";
  }

  public String getWorkspaceUriTrailingSlash(final String workspaceName) {
    return this.getWorkspacesUriTrailingSlash() + validateInput(workspaceName) + "/";
  }

  public String getWorkspaceUri(final String workspaceName) {
    return this.getWorkspacesUriTrailingSlash() + validateInput(workspaceName);
  }

  public String getArtifactsUri(final String workspaceName) {
    return this.getWorkspaceUriTrailingSlash(workspaceName) + "artifacts";
  }

  public String getArtifactsUriTrailingSlash(final String workspaceName) {
    return this.getWorkspaceUriTrailingSlash(workspaceName) + "artifacts/";
  }

  public String getArtifactUriTrailingSlash(final String workspaceName, final String artifactName) {
    final var cleanArtifactName = validateInput(artifactName);
    return this.getArtifactsUriTrailingSlash(workspaceName) + cleanArtifactName + "/";
  }

  public String getArtifactUri(final String workspaceName, final String artifactName) {
    final var cleanArtifactName = validateInput(artifactName);
    return this.getArtifactsUriTrailingSlash(workspaceName) + cleanArtifactName;
  }

  public String getArtifactUriFocusing(final String workspaceName, final String artifactName) {
    final var cleanArtifactName = validateInput(artifactName);
    return this.getArtifactsUriTrailingSlash(workspaceName) + cleanArtifactName + "/focus";
  }

  public String getAgentBodiesUri(final String workspaceName) {
    return this.getWorkspaceUriTrailingSlash(workspaceName) + "artifacts/";
  }

  public String getAgentBodyUriTrailingSlash(final String workspaceName, final String agentName) {
    final var cleanAgentName = validateInput(agentName);
    return this.getAgentBodiesUri(workspaceName) + "body_" + cleanAgentName + "/";
  }

  public String getAgentBodyUri(final String workspaceName, final String agentName) {
    final var cleanAgentName = validateInput(agentName);
    return this.getAgentBodiesUri(workspaceName) + "body_" + cleanAgentName;
  }

  public String getAgentUri(final String agentName) {
    final var cleanAgentName = validateInput(agentName);
    return this.baseUriTrailingSlash + "artifacts/" + cleanAgentName + "/";
  }

  // TODO: Add better validation

  /**
   * Validate the input string by removing any slashes.
   * The name cannot have any slashes since we use them as separators in the URI.
   *
   * @param stringInput The input string to validate.
   * @return The validated string.
   */
  private String validateInput(final String stringInput) {
    if (stringInput == null) {
      return "";
    }
    return stringInput.replaceAll("/", "");
  }
}

