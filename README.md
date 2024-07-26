# Project assignment

Welcome to the Documentation of the `NamespaceClass` operator assignment.

## Table of Contents

- [Operator](#operator)
    - [Overview](#overview)
    - [CRD definition](#crd-definition)
    - [Network isolation](#network-isolation)
    - [Resource management](#resource-management)
    - [Security enforcement](#security-enforcement)
    - [RBAC](#rbac)
    - [Storage isolation](#storage-isolation)
    - [Additional security considerations](#additional-security-considerations)
    - [Optional security considerations](#optional-security-considerations)
    - [Exception handling](#exception-handling)
    - [Rate Limiting](#rate-limiting)

- [Project structure](#project-structure)
    - [Reconcilers](#reconcilers)
    - [Dependents](#dependents)

- [Infra](#infra)
    - [Caching](#caching)
    - [Image building](#image-building)
    - [Observability](#observability)

- [CI/CD](#cicd)
    - [Strategy](#strategy)
    - [CI workflows](#ci-workflows)

- [Production recommendations](#production-recommendations)
    - [High Availability (HA)](#high-availability-ha)
    - [Monitoring](#monitoring)
    - [Logging](#logging)
    - [Alerting](#alerting)
    - [Security](#security)

- [Running the project](#running-the-project)
    - [Dependencies](#dependencies)
    - [Run project](#run-project)
    - [Test project](#test-project)

## Operator

### Overview

The `NamespaceClass` controller is a Kubernetes operator built using the [Java Operator SDK](https://javaoperatorsdk.io). It creates a Custom Resource
Definition (CRD) called `NamespaceClass`, which allows Kubernetes administrators to define pre-configured sets of resources, policies, and
configurations for each namespace labeled with `namespaceclass.akuity.io/name: <name-of-the-namespace-class>`. It implements a secure-by-default
approach to create and isolate namespaces using the following features:

- **Network Isolation**
- **Resource Management**
- **Security Enforcement**
- **Storage Isolation**
- **RBAC**

### CRD Definition

The Custom Resource Definition (CRD) is constructed using
the [fabric8 CRD generator](https://github.com/fabric8io/kubernetes-client/blob/main/doc/CRD-generator.md). It leverages annotations to
validate the values specified within the CRD, to ensure that the input conforms to the expected format and constraints.

Here is an example for defining an internal network:

```yaml
apiVersion: akuity.io/v1
kind: NamespaceClass
metadata:
  name: internal-network
  namespace: default
spec:
  networkPolicy:
    ingress: "10.0.0.0/16"
    egress: "10.0.0.0/16"
  
  resourceQuota:
    cpu: "8"
    memory: "16Gi"
    pods: "20"
  
  limitRange:
    defaultRequestCpu: "500m"
    defaultRequestMemory: "512Mi"
    defaultLimitCpu: "500m"
    defaultLimitMemory: "512Mi"
  
  security:
    podSecurityStandard: "baseline"
  
  storage:
    provisioner: "rancher.io/local-path"
  
  rbac:
    allowedRoles:
      - "admin"
      - "editor"
```

### Network isolation

It ensures network segmentation by using network policies to manage communication between pods and namespaces. It uses the following
strategies:

- A default deny-all policy, which blocks all traffic by default, ensuring that no unintended communication occurs.
- An optional, configurable `networkPolicy`, which allows administrators to define specific ingress and egress requirements.

### Resource management

It deploys a [ResourceQuota](https://kubernetes.io/docs/concepts/policy/resource-quotas/) and
a [LimitRange](https://kubernetes.io/docs/concepts/policy/limit-range/) to ensure fair resource distribution and prevent resource monopolization by
any single namespace. It guarantees that CPU, memory and pods are allocated equitably among namespaces (or tenant), thereby maintaining system
stability and preventing any one from consuming disproportionate resources.

### Security enforcement

It adopts [Pod Security Standards](https://kubernetes.io/docs/concepts/security/pod-security-standards/) to enforce security best practices and
restrict pod privileges across namespaces. It ensures that pods operate with the minimum necessary permissions, reducing the risk of security
vulnerabilities.

### RBAC

It creates three optional predefined roles (viewer, editor, and admin) within their respective namespaces to manage access control effectively. The
viewer role provides read-only access, the editor role allows for modification of resources, and the admin role grants full
control over the namespace.

### Storage isolation

It defines a [StorageClass](https://kubernetes.io/docs/concepts/storage/storage-classes/) to ensure data isolation for each tenant. By using specific
storage classes, it guarantees that each tenant's data is stored separately, preventing data leakage and ensuring compliance with data
protection standards.

### Additional security considerations

- **Admission Controllers**: Use [OPA](https://www.openpolicyagent.org/) or [Kyverno](https://kyverno.io/) to enforce policies, including RBAC and
  storage usage for each namespace, as well as any other rules you want to enforce per namespace or tenant.
- **Service Mesh**: Use a service mesh like [linkerd](https://linkerd.io/) or [cilium](https://cilium.io/) (much more sophisticated) to provide
  advanced network security controls, traffic management, mTLS [...].
- **Secret Encryption**: Encrypt secret data in etcd to protect sensitive information, to make sure the secrets are securely stored and managed within
  the Kubernetes environment.

### Optional security considerations

- **Node Isolation**: Implement node affinity to achieve node-level isolation, ensuring that workloads are assigned to specific tenant nodes based on
  defined criteria.
- **Virtual Clusters**: If you need complete tenant isolation, use virtual clusters to ensure that every tenant operates within a fully isolated and
  independent Kubernetes environment.

### Exception Handling

The controller schedules an automatic retry of the reconciliation process whenever an exception is thrown during reconciliation. By default, it will
make 5 retry attempts with an exponential backoff strategy, starting with an interval of 2 seconds.

### Rate Limiting

I did not implement rate limiting for the controller. However, if needed, you can use the following annotation:

```java
@RateLimited(maxReconciliations = 2, within = 3, unit = TimeUnit.SECONDS);
```

This annotation will limit the controller to a maximum of 2 reconciliations within 3 seconds. Adapt it to the specific needs of the project.

## Project structure

The project hierarchy adheres to standard Java package conventions, organized by package type.

```
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── akuity
│   │   │           ├── conditions
│   │   │           │   ├── HasValidNamespaceClassAndAdminRole.java
│   │   │           │   ├── HasValidNamespaceClassAndEditorRole.java
│   │   │           │   ├── HasValidNamespaceClassAndNetworkPolicy.java
│   │   │           │   ├── HasValidNamespaceClassAndViewerRole.java
│   │   │           │   └── HasValidNamespaceClass.java
│   │   │           ├── customresources
│   │   │           │   ├── NamespaceClass.java
│   │   │           │   ├── NamespaceClassSpec.java
│   │   │           │   └── NamespaceClassStatus.java
│   │   │           ├── dependents
│   │   │           │   ├── LimitRangeDR.java
│   │   │           │   ├── NetworkPolicyDenyAllByDefaultDR.java
│   │   │           │   ├── NetworkPolicyDR.java
│   │   │           │   ├── ResourceQuotaDR.java
│   │   │           │   ├── RoleAdminDR.java
│   │   │           │   ├── RoleBindingAdminDR.java
│   │   │           │   ├── RoleBindingEditorDR.java
│   │   │           │   ├── RoleBindingViewerDR.java
│   │   │           │   ├── RoleEditorDR.java
│   │   │           │   ├── RoleViewerDR.java
│   │   │           │   ├── ServiceAccountAdminDR.java
│   │   │           │   ├── ServiceAccountEditorDR.java
│   │   │           │   ├── ServiceAccountViewerDR.java
│   │   │           │   └── StorageClassDR.java
│   │   │           ├── reconcilers
│   │   │           │   └── NamespaceReconciler.java
│   │   │           └── utils
│   │   │               ├── ConditionUtils.java
│   │   │               ├── Constants.java
│   │   │               ├── NamespaceUtils.java
│   │   │               └── ResourceUtils.java
│   │   └── resources
│   │       └── application.properties
```

### Reconcilers

The primary resource defined in the reconciler is the `Namespace`, as the goal is to manage related resources per namespace. During each
namespace reconciliation, the following labels are added/removed:

- `pod-security.kubernetes.io/enforce-version: latest`
- `pod-security.kubernetes.io/enforce: <security.podSecurityStandard>`
- `namespaceclass.akuity.io/ready: <true/false>`

Since the controller does not directly manage the `NamespaceClass` object, it configures an `InformerEventSource` to reconcile all namespaces whenever
a `NamespaceClass` is created, updated, or deleted. Because `NamespaceClass` objects are not managed directly, it does not store any owner references,
which invalidates the `getSecondaryResource` method. Instead, it implements a thread-safe HashMap used as a cache to store all `NamespaceClass`
instances. This is crucial because it ensures that namespaces with the label `namespaceclass.akuity.io/name` are always reconciled with the
corresponding `NamespaceClass`. The cache is updated using three filters: onAdd, onUpdate, and onDelete. Additionally, it implements
a `withSecondaryToPrimaryMapper` to reconcile all namespaces in order to detect label creation, updates, and removals.

```java
private InformerEventSource<NamespaceClass, Namespace> buildNamespaceClassInformerEventSource(final EventSourceContext<Namespace> context) {
  return new InformerEventSource<>(
      InformerConfiguration.from(NamespaceClass.class, context)
          // There is no owner reference this is why we need to use the cache to map nsClass from namespace.
          .withPrimaryToSecondaryMapper(primary -> new HashSet<>(namespaceClassCache.values()))
          .withSecondaryToPrimaryMapper(
              secondary -> NamespaceUtils.buildNamespaceResourceIDsFromCache(context))
          .withOnAddFilter(
              secondary -> {
                addToCache(secondary);
                return true;
              })
          .withOnUpdateFilter(
              (oldSecondary, secondary) -> {
                removeFromCache(oldSecondary);
                addToCache(secondary);
                return true;
              })
          .withOnDeleteFilter(
              (secondary, b) -> {
                removeFromCache(secondary);
                return true;
              })
          .build(),
      context);
```

I did not implement a function to update the `NamespaceClass` status, but it can be easily done by iterating over all cached namespaces and checking
the `namespaceclass.akuity.io/ready` label. To prevent any unnecessary calls to the Kubernetes API, I would recommend to only update the status if it
was updated.

### Dependents

All dependent resources use a `labelSelector` with a value of `app.kubernetes.io/namespace-class` to ensure that only the resources managed by the
controller are selected.

These resources are created, updated, or deleted based on a reconciliation condition that checks if the namespace has a valid
label `namespaceclass.akuity.io/name: <name-of-the-namespace-class>` value. If this label is not present (or invalid), the corresponding resources are
deleted. Additionally, since the `networkPolicy` and the three RBAC roles are optional, their presence are verified, and if not found, deleted.

They all override the `toPrimaryResourceIDs` method to retrieve their respective primary resources and implement a resource
`Discriminator` for the controller to uniquely identify them.

By default, every modification will trigger a reconciliation of the namespace.

## Infra

### Caching

The Operator SDK implements an in-memory cache to optimize reconciliation. Additionally, every interaction between primary and secondary resources has
been implemented to maximize cache use and avoid unnecessary calls to the Kubernetes API server. Furthermore, as explained
in [Reconcilers](#reconcilers), all interactions with `NamespaceClass` have also been optimized to use an in-memory cache. If needed, for better
memory management, the `BoundedItemStore` can be extended to implement a centralized cache like [Redis](https://redis.io)
or [Valkey](https://valkey.io/).

### Image building

The application utilizes [Buildpack](https://buildpacks.io/) to construct a production-ready image
optimized for runtime efficiency, embedding security best practices and resource-aware container
capabilities.

### Observability

The controller is automatically instrumented with the [Micrometer metrics extension](https://quarkus.io/guides/telemetry-micrometer) in Prometheus
format. In addition, the Java Operator SDK [automatically](https://javaoperatorsdk.io/docs/features/#micrometer-implementation) provides
reconciliations metrics out of the box, such as `operator.sdk.reconciliations.failed` and `operator.sdk.reconciliations.success`.

## CI/CD

### Strategy

I did not have sufficient time to test every condition and behavior, but it can be implemented quite easily as all tests are implemented generically
inside the `KubernetesDependentResourceBaseTest.class`. You only needs to override specific functions in each resource class.

For example the `NetworkPolicyDRIT`:

```java
class NetworkPolicyDRIT extends KubernetesDependentResourceBaseTest<NetworkPolicy> {

  @RegisterExtension
  static LocallyRunOperatorExtension operator =
      LocallyRunOperatorExtension.builder()
          .waitForNamespaceDeletion(false)
          .oneNamespacePerClass(true)
          .withReconciler(new NamespaceReconciler())
          .withKubernetesClient(new KubernetesClientBuilder().build())
          .withAdditionalCustomResourceDefinition(NamespaceClass.class)
          .build();

  @Override
  public LocallyRunOperatorExtension operator() {
    return operator;
  }

  @Override
  public Class<NetworkPolicy> getDependentResourceClass() {
    return NetworkPolicy.class;
  }

  @Override
  public String getResourceName() {
    return ResourceUtils.name(operator.getNamespace(), NetworkPolicyDR.COMPONENT);
  }

  @Override
  public void dependentResourceAssertion(NetworkPolicy resource, NamespaceClass nsClass) {
    assertThat(resource.getSpec().getEgress().get(0).getTo().get(0).getIpBlock().getCidr())
        .isEqualTo(nsClass.getSpec().getNetworkPolicy().getEgress());
  }

  @Override
  public NetworkPolicy getDependentResourceToUpdate() {
    var resourceToUpdate = operator().get(getDependentResourceClass(), getResourceName());
    resourceToUpdate
        .getSpec()
        .getEgress()
        .get(0)
        .setTo(
            List.of(
                new NetworkPolicyPeerBuilder()
                    .withIpBlock(new IPBlockBuilder().withCidr("1.2.3.4/1").build())
                    .build()));
    return resourceToUpdate;
  }
}
```

With more time I would have also implemented many more unit tests.

### CI Workflows

Two simple workflows are defined inside the `/.github/workflows` directory. One workflow is triggered on every pull request interaction, and the other
on pushes to the develop branch. Both workflows use Minikube to launch the integration tests.

## Production recommendations

### High Availability (HA)

Operators are generally deployed with a single running or active instance. However, the `Operator` can be configured
with `withLeaderElectionConfiguration` to define a leader election configuration. This ensures that only one instance, the leader, handles the
reconciliation and event processing, while others remain on standby, ready to take over if the leader fails.

If you follow this path, ensure high availability (HA) with the following:

- **Replicas**: Maintain a minimum of 2 replicas.
- **Autoscaling**: Implement autoscaling based on CPU and memory metrics (or other) to efficiently
  handle varying workload demands.
- **Anti-Affinity**: Create an anti-affinity rule to distribute replicas across different nodes
  or zones, improving fault tolerance.
- **PodDisruptionBudget**: Create a `PodDisruptionBudget` with at least 2 replicas to prevent
  issues or failures during Kubernetes operations.

### Monitoring

- Use an OpenTelemetry [Collector](https://opentelemetry.io/docs/collector/) to consolidate all your metrics and traces. This approach helps maintain
  vendor agnosticism and provides a centralized location for collecting and processing telemetry data.
- Use a Grafana dashboard to monitor your controller resources and reconciliation metrics, with a particular focus on the failed ones.

### Logging

- Monitor your warning and error logs daily to proactively detect API errors.

### Alerting

I strongly recommend creating an alerting rule based on the failed metrics `operator.sdk.controllers.execution.reconcile.failure`,
`operator.sdk.reconciliations.failed` and queue length `operator.sdk.reconciliations.queue.size`. Here is an example:

```yaml
        groups:
          - name: operator-sdk-alerts
            rules:
              - alert: NamespaceClassControllerReconciliationFailed
                expr: operator_sdk_reconciliations_failed > 0
                for: 5m
                labels:
                  severity: critical
                annotations:
                  summary: "Reconciliation failed"
                  description: "Reconciliation failed for namespace {{ $labels.namespace }}"
              
              - alert: NamespaceClassControllerReconciliationQueueLengthIsHigh
                expr: operator_sdk_reconciliations_queue_size > 10
                for: 5m
                labels:
                  severity: warning
                annotations:
                  summary: "High reconciliation queue length"
                  description: "The reconciliation queue length has exceeded 10 for the past 5 minutes."
```

### Security

To enhance security, consider implementing the following measures:

- **Metrics port**: Avoid exposing the metrics port externally; limit access to `serviceMonitor` only.
- **Secret Management**: Retrieve secrets securely from Vault using a secret operator.
- **Service Accounts with Workload Identity**: Always utilize service accounts with workload
  identity for secure access control.
- **Code Quality** Use tools for code quality and static analysis like SonarQube
- **Deployment**: Use secure Kubernetes deployment like the following:

```yaml
    spec:
      containers:
        - name: api
          image: "<your-image-registry>"
          ports:
            - name: container-port
              containerPort: 8080
            - name: metrics
              containerPort: 8081
          
          readinessProbe:
            initialDelaySeconds: 15
            periodSeconds: 10
            httpGet:
              path: /q/health/ready
              port: metrics
          livenessProbe:
            httpGet:
              path: /q/health/live
              port: metrics
            initialDelaySeconds: 15
            periodSeconds: 10
          
          lifecycle:
            preStop:
              exec:
                command:
                  - sleep
                  - 10
          
          # Uses adequate requests resources. Preferably set requests equal to limits.
          resources:
            requests:
              memory: 768Mi
              cpu: 500m
            limits:
              memory: 768Mi
          
          # Tighten security context
          securityContext:
            readOnlyRootFilesystem: true
            allowPrivilegeEscalation: false
            capabilities:
              drop:
                - ALL
            runAsNonRoot: true
            runAsUser: 1000
          
          volumeMounts:
            - name: tmp-volume
              mountPath: /tmp
      
      # Necessary for readOnly system
      volumes:
        - name: tmp-volume
          emptyDir: { }    
```

## Running the project

### Dependencies

The dependencies of the project are:

* OpenJDK Java version >= 21
* [Minikube](https://minikube.sigs.k8s.io/docs/) v1.33.1
* [Kubernetes](https://kubernetes.io/) v1.30.3
* [Maven](https://maven.apache.org/)

### Run project

Have your local cluster available and run:

```bash
mvn clean package && mvn quarkus:dev
```

Quarkus will automatically deploy the CRD and the operator. You will then need to deploy the following YAML files (available inside the `kubernetes`
folder):

NamespaceClass: internal-network

```yaml
apiVersion: akuity.io/v1
kind: NamespaceClass
metadata:
  name: internal-network
  namespace: default
spec:
  networkPolicy:
    ingress: "10.0.0.0/16"
    egress: "10.0.0.0/16"
  
  resourceQuota:
    cpu: "8"
    memory: "16Gi"
    pods: "20"
  
  limitRange:
    defaultRequestCpu: "500m"
    defaultRequestMemory: "512Mi"
    defaultLimitCpu: "500m"
    defaultLimitMemory: "512Mi"
  
  security:
    podSecurityStandard: "baseline"
  
  storage:
    provisioner: "rancher.io/local-path"
  
  rbac:
    allowedRoles:
      - "admin"
      - "editor"
```

NamespaceClass: public-network:

```yaml
apiVersion: akuity.io/v1
kind: NamespaceClass
metadata:
  name: public-network
  namespace: default
spec:
  networkPolicy:
    ingress: "0.0.0.0/0"
  
  resourceQuota:
    cpu: "8"
    memory: "16Gi"
    pods: "20"
  
  limitRange:
    defaultRequestCpu: "500m"
    defaultRequestMemory: "512Mi"
    defaultLimitCpu: "500m"
    defaultLimitMemory: "512Mi"
  
  security:
    podSecurityStandard: "restricted"
  
  storage:
    provisioner: "rancher.io/local-path"
  
  rbac:
    allowedRoles:
      - "viewer"
```

Namespace: internal

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: internal
  labels:
    namespaceclass.akuity.io/name: internal-network
```

Namespace: public

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: public
  labels:
    namespaceclass.akuity.io/name: public-network
```

Testing all controller reconciliation scenarios:

Namespace:

- Update `Namespace` label `namespaceclass.akuity.io/name` to an invalid value.
- Update `Namespace` label `namespaceclass.akuity.io/name` to another class.
- Remove `Namespace` label `namespaceclass.akuity.io/name`.
- Add a new `Namespace` with label `namespaceclass.akuity.io/name`.

NamespaceClass:

- Update `networkPolicy` value.
- Remove `networkPolicy` value.
- Test the same function with rbac values.
- Delete `NamespaceClass`.

Dependent:

- Update any value from the dependent resources like `LimitRange` for example.
- Remove any dependent resource.

### Test project

Make sure you have a locally running cluster and run:

```yaml
mvn clean verify
```