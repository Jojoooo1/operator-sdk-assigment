package com.akuity.testutils;

import com.akuity.customresources.NamespaceClass;
import com.akuity.customresources.NamespaceClassSpec;
import com.akuity.customresources.NamespaceClassSpec.LimitRange;
import com.akuity.customresources.NamespaceClassSpec.NetworkPolicy;
import com.akuity.customresources.NamespaceClassSpec.PodSecurityProfile;
import com.akuity.customresources.NamespaceClassSpec.RBAC;
import com.akuity.customresources.NamespaceClassSpec.ResourceQuota;
import com.akuity.customresources.NamespaceClassSpec.RoleEnum;
import com.akuity.customresources.NamespaceClassSpec.Security;
import com.akuity.customresources.NamespaceClassSpec.Storage;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import java.util.List;

public class TestBuilder {

  public static NamespaceClass buildNsClass(String name, String namespace) {
    NamespaceClass namespaceClass = new NamespaceClass();

    // Set metadata
    namespaceClass.setMetadata(
        new ObjectMetaBuilder().withName(name).withNamespace(namespace).build());

    // Create and set spec
    NamespaceClassSpec spec = new NamespaceClassSpec();

    // Set networkPolicy
    NetworkPolicy networkPolicy = new NetworkPolicy();
    networkPolicy.setIngress("10.0.0.0/16");
    networkPolicy.setEgress("10.0.0.0/16");
    spec.setNetworkPolicy(networkPolicy);

    // Set resourceQuota
    ResourceQuota resourceQuota = new ResourceQuota();
    resourceQuota.setCpu("8");
    resourceQuota.setMemory("16Gi");
    resourceQuota.setPods("20");
    spec.setResourceQuota(resourceQuota);

    // Set limitRange
    LimitRange limitRange = new LimitRange();
    limitRange.setDefaultRequestCpu("500m");
    limitRange.setDefaultRequestMemory("512Mi");
    limitRange.setDefaultLimitCpu("500m");
    limitRange.setDefaultLimitMemory("512Mi");
    spec.setLimitRange(limitRange);

    // Set security
    Security security = new Security();
    security.setPodSecurityStandard(PodSecurityProfile.BASELINE);
    spec.setSecurity(security);

    // Set storage
    Storage storage = new Storage();
    storage.setProvisioner("rancher.io/local-path");
    spec.setStorage(storage);

    // Set rbac
    RBAC rbac = new RBAC();
    rbac.setAllowedRoles(List.of(RoleEnum.ADMIN));
    spec.setRbac(rbac);

    namespaceClass.setSpec(spec);

    return namespaceClass;
  }
}
