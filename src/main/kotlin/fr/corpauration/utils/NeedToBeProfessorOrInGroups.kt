package fr.corpauration.utils

import javax.enterprise.util.Nonbinding
import javax.interceptor.InterceptorBinding

@InterceptorBinding
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.TYPE, AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
annotation class NeedToBeProfessorOrInGroups(@get:Nonbinding vararg val groups: Int)
