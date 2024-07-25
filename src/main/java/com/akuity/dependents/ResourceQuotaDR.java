package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.NamespaceUtils;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceQuota;
import io.fabric8.kubernetes.api.model.ResourceQuotaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = ResourceQuotaDR.Discriminator.class)
public class ResourceQuotaDR extends CRUDKubernetesDependentResource<ResourceQuota, Namespace>
    implements SecondaryToPrimaryMapper<ResourceQuota> {

  public static final String COMPONENT = "resource-quota";

  public ResourceQuotaDR() {
    super(ResourceQuota.class);
  }

  @Override
  public ResourceQuota desired(Namespace primary, Context<Namespace> context) {

    var namespaceClass =
        NamespaceUtils.getNamespaceClassFromNamespaceOrThrowException(context, primary);
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();

    var quota = namespaceClass.getSpec().getResourceQuota();
    Map<String, Quantity> hard =
        Map.of(
            "requests.cpu",
            new Quantity(quota.getCpu()),
            "limits.cpu",
            new Quantity(quota.getCpu()),
            "requests.memory",
            new Quantity(quota.getMemory()),
            "limits.memory",
            new Quantity(quota.getMemory()),
            "pods",
            new Quantity(quota.getPods()));

    return new ResourceQuotaBuilder()
        .withMetadata(meta)
        .withNewSpec()
        .withHard(hard)
        .endSpec()
        .build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final ResourceQuota secondary) {
    return Set.of(new ResourceID(secondary.getMetadata().getNamespace()));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator
      extends ResourceIDMatcherDiscriminator<ResourceQuota, Namespace> {
    public Discriminator() {
      super(
          COMPONENT,
          primary ->
              new ResourceID(
                  ResourceUtils.name(primary, COMPONENT), ResourceUtils.namespace(primary)));
    }
  }
}
