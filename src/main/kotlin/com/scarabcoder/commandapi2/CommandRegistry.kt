package com.scarabcoder.commandapi2

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.command.defaults.BukkitCommand
import java.lang.reflect.Field
import java.util.logging.Level
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation


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
object CommandRegistry {

    private val commands: HashMap<String, CommandSection> = HashMap()

    fun registerCommand(section: CommandSection){
        commands.put(section.name, section)

        val cmdMap: Field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        cmdMap.isAccessible = true

        val serverCmds: CommandMap = cmdMap.get(Bukkit.getServer()) as CommandMap
        serverCmds.register(section.name, CommandHandler.BukkitCommand(section))
    }

    fun getCmdUsage(func: KCallable<*>): String {
        val usage = StringBuilder()
        usage.append("${func.name} ")
        for(param in func.parameters.subList(2, func.parameters.size)){
            usage.append("<${getParamName(param)}> ")
        }
        return usage.toString()
    }


    fun getParamName(param: KParameter): String{
        val arg = param.findAnnotation<Argument>() ?: return param.name!!
        return if(arg.name == "") param.name!! else arg.name
    }



}