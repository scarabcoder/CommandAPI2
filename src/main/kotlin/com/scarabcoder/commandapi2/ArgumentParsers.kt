package com.scarabcoder.commandapi2

import com.scarabcoder.commandapi2.exception.ArgumentParseException
import com.scarabcoder.commandapi2.exception.CommandException
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.HashMap
import java.util.function.Function
import kotlin.reflect.KClass


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

    private val arguments = HashMap<KClass<*>, Function<String, Any?>>()
    private val senderArgs = HashMap<KClass<*>, Function<CommandSender, Any>>()

    init {

        arguments.put(Int::class, Function {
            t: String ->  {
            try {
                t.toInt()
            } catch(e: NumberFormatException) {
                throw ArgumentParseException("$t is not a number!")
            }
        }.invoke()
        })
        arguments.put(Double::class, Function {
            t: String ->  {
            try {
                t.toDouble()
            } catch(e: NumberFormatException) {
                throw ArgumentParseException("$t is not a number!")
            }
        }.invoke()
        })
        arguments.put(Player::class, Function {
            t: String ->  {
            val p: Player? = Bukkit.getPlayer(t) ?: throw ArgumentParseException("Player \"$t\" not found!")
            p

        }.invoke()
        })



    }

    fun parseSender(sender: CommandSender, to: KClass<*>): Any {
        if(!supportsSender(to)) throw IllegalArgumentException("Cannot cast to type $to!")
        return senderArgs[to]!!.apply(sender)
    }

    internal fun supportsSender(type: KClass<*>): Boolean{
        return senderArgs.containsKey(type)
    }

    fun registerArgument(type: KClass<*>, func: Function<String, Any?>){
        arguments.put(type, func)
    }

    fun registerSenderType(type: KClass<*>, func: Function<CommandSender, Any>){
        senderArgs.put(type, func)
    }

    @Throws(ArgumentParseException::class)
    internal fun parse(arg:String, clazz: KClass<*>): Any? {
        if(arguments.containsKey(clazz)){
            return arguments[clazz]!!.apply(arg)
        }
        throw CommandException("Could not find a casting function for String -> " + clazz.simpleName + " (contact a developer)")
    }

}