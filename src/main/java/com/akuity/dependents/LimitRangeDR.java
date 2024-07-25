package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.NamespaceUtils;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.LimitRange;
import io.fabric8.kubernetes.api.model.LimitRangeBuilder;
import io.fabric8.kubernetes.api.model.LimitRangeItemBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = LimitRangeDR.Discriminator.class)
public class LimitRangeDR extends CRUDKubernetesDependentResource<LimitRange, Namespace>
    implements SecondaryToPrimaryMapper<LimitRange> {

  public static final String COMPONENT = "limit-range";

  public LimitRangeDR() {
    super(LimitRange.class);
  }

  @Override
  public LimitRange desired(final Namespace primary, final Context<Namespace> context) {

    var namespaceClass =
        NamespaceUtils.getNamespaceClassFromNamespaceOrThrowException(context, primary);
    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();

    var limitRange = namespaceClass.getSpec().getLimitRange();

    Map<String, Quantity> defaultRequest =
        Map.of(
            "cpu",
            new Quantity(limitRange.getDefaultRequestCpu()),
            "memory",
            new Quantity(limitRange.getDefaultRequestMemory()));
    Map<String, Quantity> defaultLimit =
        Map.of(
            "cpu",
            new Quantity(limitRange.getDefaultLimitCpu()),
            "memory",
            new Quantity(limitRange.getDefaultLimitMemory()));

    return new LimitRangeBuilder()
        .withMetadata(meta)
        .withNewSpec()
        .withLimits(
            Collections.singletonList(
                new LimitRangeItemBuilder()
                    .withType("Container")
                    .withDefaultRequest(defaultRequest)
                    .withDefault(defaultLimit)
                    .build()))
        .endSpec()
        .build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final LimitRange secondary) {
    return Set.of(new ResourceID(secondary.getMetadata().getNamespace()));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator extends ResourceIDMatcherDiscriminator<LimitRange, Namespace> {
    public Discriminator() {
      super(
          COMPONENT,
          primary ->
              new ResourceID(
                  ResourceUtils.name(primary, COMPONENT), ResourceUtils.namespace(primary)));
    }
  }
}
