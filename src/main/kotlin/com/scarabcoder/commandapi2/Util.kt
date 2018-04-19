package com.scarabcoder.commandapi2

import com.google.common.primitives.Ints
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation



fun String.keepSpaceAfter(): String {
    var spaced = this
    if(!spaced.endsWith(" ") && spaced != "") spaced += " "
    return spaced
}

fun KClass<*>.findMember(name: String): KCallable<*>? {
    return this.members.find { it.name == name }
}

inline fun <reified T : Annotation> KCallable<*>.hasAnnotation(): Boolean {
    return this.findAnnotation<T>() != null
}

fun List<*>.subList(from: Int): List<*> {
    return this.subList(from, this.size)
}