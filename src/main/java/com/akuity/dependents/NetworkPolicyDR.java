package com.akuity.dependents;

import static com.akuity.utils.Constants.K8S_NAMESPACE_CLASS;

import com.akuity.utils.NamespaceUtils;
import com.akuity.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.networking.v1.IPBlockBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyEgressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeerBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.SecondaryToPrimaryMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@KubernetesDependent(
    labelSelector = K8S_NAMESPACE_CLASS,
    resourceDiscriminator = NetworkPolicyDR.Discriminator.class)
public class NetworkPolicyDR extends CRUDKubernetesDependentResource<NetworkPolicy, Namespace>
    implements SecondaryToPrimaryMapper<NetworkPolicy> {

  public static final String COMPONENT = "network-policy";

  public NetworkPolicyDR() {
    super(NetworkPolicy.class);
  }

  @Override
  public NetworkPolicy desired(Namespace primary, Context<Namespace> context) {

    var namespaceClass =
        NamespaceUtils.getNamespaceClassFromNamespaceOrThrowException(context, primary);

    var meta = ResourceUtils.fromResource(primary, COMPONENT).build();
    var policyBuilder =
        new NetworkPolicyBuilder()
            .withMetadata(meta)
            .withNewSpec()
            // Select all pods in the namespace;
            .withPodSelector(new LabelSelectorBuilder().build());

    List<String> policyTypes = new ArrayList<>();

    var ingress = namespaceClass.getSpec().getNetworkPolicy().getIngress();
    if (ingress != null) {
      policyBuilder.addToIngress(
          new NetworkPolicyIngressRuleBuilder()
              .addToFrom(
                  new NetworkPolicyPeerBuilder()
                      .withIpBlock(new IPBlockBuilder().withCidr(ingress).build())
                      .build())
              .build());
      policyTypes.add("Ingress");
    }

    var egress = namespaceClass.getSpec().getNetworkPolicy().getEgress();
    if (egress != null) {
      policyBuilder.addToEgress(
          new NetworkPolicyEgressRuleBuilder()
              .addToTo(
                  new NetworkPolicyPeerBuilder()
                      .withIpBlock(new IPBlockBuilder().withCidr(egress).build())
                      .build())
              .build());
      policyTypes.add("Egress");
    }

    if (!policyTypes.isEmpty()) {
      policyBuilder.withPolicyTypes(policyTypes);
    }

    return policyBuilder.and().build();
  }

  /*
   *  Necessary to map secondary resources to primary resource.
   *  It only uses the namespace name to have consistent mapping.
   * */
  @Override
  public Set<ResourceID> toPrimaryResourceIDs(final NetworkPolicy secondary) {
    return Set.of(new ResourceID(secondary.getMetadata().getNamespace()));
  }

  /*
   *  Necessary since the controller is managing multiple k8s resources
   * */
  public static class Discriminator
      extends ResourceIDMatcherDiscriminator<NetworkPolicy, Namespace> {
    public Discriminator() {
      super(
          COMPONENT,
          primary ->
              new ResourceID(
                  ResourceUtils.name(primary, COMPONENT), ResourceUtils.namespace(primary)));
    }
  }
}
