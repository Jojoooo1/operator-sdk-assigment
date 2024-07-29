package com.akuity.utils;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Constants {

  public static final String OPERATOR_NAME = "namespace-class-operator";

  public static final String NAMESPACE_IS_SYNC_LABEL_KEY = "namespaceclass.akuity.io/ready";
  public static final String NAMESPACE_POD_SECURITY_ENFORCE_VERSION_LABEL_KEY =
      "pod-security.kubernetes.io/enforce-version";
  public static final String NAMESPACE_POD_SECURITY_ENFORCE_LABEL_KEY =
      "pod-security.kubernetes.io/enforce";

  public static final String K8S_NAME = "app.kubernetes.io/name";
  public static final String K8S_COMPONENT = "app.kubernetes.io/component";
  public static final String K8S_NAMESPACE_CLASS = "app.kubernetes.io/namespace-class";
  public static final String K8S_MANAGED_BY = "app.kubernetes.io/managed-by";

  public static final String K8S_ARGOCD_INSTANCE = "argocd.argoproj.io/instance";
}
