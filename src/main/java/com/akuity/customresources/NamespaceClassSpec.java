package com.akuity.customresources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import io.fabric8.generator.annotation.Nullable;
import io.fabric8.generator.annotation.Required;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class NamespaceClassSpec {

  @Nullable
  @JsonPropertyDescription("Network policy configuration for the namespace")
  private NetworkPolicy networkPolicy;

  @Required
  @JsonPropertyDescription("Resource quota configuration for the namespace")
  private ResourceQuota resourceQuota;

  @Required
  @JsonPropertyDescription("Limit range configuration for the namespace")
  private LimitRange limitRange;

  @Required
  @JsonPropertyDescription("Security configuration for the namespace")
  private Security security;

  @Required
  @JsonPropertyDescription("Storage configuration for the namespace")
  private Storage storage;

  @Nullable
  @JsonPropertyDescription("RBAC configuration for the namespace")
  private RBAC rbac;

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public enum RoleEnum {
    @JsonProperty("admin")
    ADMIN("admin"),
    @JsonProperty("editor")
    EDITOR("editor"),
    @JsonProperty("viewer")
    VIEWER("viewer");

    private final String role;
  }

  @Getter
  @AllArgsConstructor(access = AccessLevel.PRIVATE)
  public enum PodSecurityProfile {
    @JsonProperty("privileged")
    PRIVILEGED("privileged"),
    @JsonProperty("baseline")
    BASELINE("baseline"),
    @JsonProperty("restricted")
    RESTRICTED("restricted");

    private final String profile;
  }

  @Setter
  @Getter
  public static class NetworkPolicy {
    @Nullable
    @JsonPropertyDescription("CIDR for ingress traffic. Example: 10.0.0.0/16")
    private String ingress;

    @Nullable
    @JsonPropertyDescription("CIDR for egress traffic. Example: 10.0.0.0/16")
    private String egress;
  }

  @Setter
  @Getter
  public static class ResourceQuota {
    @Required
    @JsonPropertyDescription("CPU quota for the namespace. Example: 2 or 2000m")
    private String cpu;

    @Required
    @JsonPropertyDescription("Memory quota for the namespace. Example: 4Gi")
    private String memory;

    @Required
    @JsonPropertyDescription("Maximum number of pods allowed in the namespace")
    private String pods;
  }

  @Setter
  @Getter
  public static class LimitRange {
    @Required
    @JsonPropertyDescription("Default CPU request for containers. Example: 100m")
    private String defaultRequestCpu;

    @Required
    @JsonPropertyDescription("Default CPU limit for containers. Example: 200m")
    private String defaultLimitCpu;

    @Required
    @JsonPropertyDescription("Default memory request for containers. Example: 128Mi")
    private String defaultRequestMemory;

    @Required
    @JsonPropertyDescription("Default memory limit for containers. Example: 256Mi")
    private String defaultLimitMemory;
  }

  @Setter
  @Getter
  public static class Security {
    @Required
    @JsonPropertyDescription(
        "Pod Security Standard to apply to the namespace (privileged, baseline, or restricted)")
    private PodSecurityProfile podSecurityStandard;
  }

  @Setter
  @Getter
  public static class Storage {
    @Required
    @JsonPropertyDescription("Storage provisioner to use for the namespace")
    private String provisioner;
  }

  @Getter
  @Setter
  public static class RBAC {
    @Nullable
    @JsonPropertyDescription("Roles to use for the namespace")
    private List<RoleEnum> allowedRoles;
  }
}
