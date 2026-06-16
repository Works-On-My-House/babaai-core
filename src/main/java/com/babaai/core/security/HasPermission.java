package com.babaai.core.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Method/type-level authorization on a single permission.
 *
 * <p>A meta-annotation over {@link PreAuthorize}: {@code @HasPermission(AppPermission.RECIPE_VERIFY)}
 * expands to {@code @PreAuthorize("hasAuthority('RECIPE_VERIFY')")} (the {@code {value}} template
 * renders the enum name). Authorities are the user's effective permission keys (see
 * {@code PermissionResolver}/{@code AuthenticatedUser}); the template is resolved by the
 * {@code AnnotationTemplateExpressionDefaults} bean in {@code SecurityConfig}.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAuthority('{value}')")
public @interface HasPermission {

    AppPermission value();
}
