package com.scarabcoder.commandapi2

import com.scarabcoder.commandapi2.exception.ArgumentParseException
import com.scarabcoder.commandapi2.exception.CommandException
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.HashMap
import java.util.function.Function
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf


/*
 * The MIT License
 *
 * Copyright 2018 Nicholas Harris
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
object ArgumentParsers {

    private val arguments = HashMap<KClass<*>, (String) -> Any>()
    private val senderArgs = HashMap<KClass<*>, (CommandSender) -> Any>()
    private val test = HashMap<KClass<*>, (String) -> Any>()

    init {
        arguments.put(Int::class, {
            if(it.toIntOrNull() == null) throw ArgumentParseException("$it is not a number!")
            it.toInt()
        })
        arguments.put(Double::class, {
            if(it.toDoubleOrNull() == null) throw ArgumentParseException("$it is not a valid number!")
            it.toDouble()
        })
        arguments.put(Player::class, {
            if(Bukkit.getPlayer(it) == null) throw ArgumentParseException("Player $it not found!")
            Bukkit.getPlayer(it)
        })



    }



    fun parseSender(sender: CommandSender, to: KClass<*>): Any {
        if(!supportsSender(to)) throw IllegalArgumentException("Cannot cast to type $to!")
        return senderArgs[to]!!.invoke(sender)
    }

    internal fun supportsSender(type: KClass<*>): Boolean{
        return senderArgs.containsKey(type)
    }

    fun registerArgument(type: KClass<*>, func: (String) -> Any){
        arguments.put(type, func)
    }

    fun registerSenderType(type: KClass<*>, func: (CommandSender) -> Any){
        senderArgs.put(type, func)
    }

    @Throws(ArgumentParseException::class)
    internal fun parse(arg:String, clazz: KClass<*>): Any? {
        if(arguments.containsKey(clazz)){
            return arguments[clazz]!!.invoke(arg)
        }
        if(clazz.java.isEnum){
            return clazz.java.enumConstants.firstOrNull { (it as Enum<*>).name.equals(arg, true) } ?:
                    throw ArgumentParseException("Possible values: " + clazz.java.enumConstants.joinToString(separator = " ").toLowerCase())
        }
        throw CommandException("Could not find a casting function for String -> " + clazz.simpleName + " (contact a developer)")
    }

}