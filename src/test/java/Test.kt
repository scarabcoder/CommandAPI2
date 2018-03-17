import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

enum class TestEnum {
    TEST1, TEST2
}
fun findEnumValue(clazz: KClass<*>, key: String): Any {

    if(clazz.isSubclassOf(Enum::class)){
        return clazz.java.enumConstants.firstOrNull { (it as Enum<*>).name.equals(key, true) } ?: "None"
    }
    return "Not enum"
}

fun main(args: Array<out String> ){

    println(findEnumValue(TestEnum::class, "TEST1")::class)

    println(TestEnum.values().toMutableList())
}