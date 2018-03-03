package com.scarabcoder.commandapi2

import com.google.common.primitives.Ints
import com.scarabcoder.commandapi2.CommandRegistry.getCmdUsage
import com.scarabcoder.commandapi2.CommandRegistry.getParamName
import com.scarabcoder.commandapi2.exception.ArgumentParseException
import com.scarabcoder.commandapi2.exception.CommandException
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.full.memberFunctions


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
internal object CommandHandler {

    class BukkitCommand(val section: CommandSection): Command(section.name, section.description, section.usage, section.aliases){
        override fun execute(sender: CommandSender?, cmd: String?, args: Array<out String>?): Boolean {
            val args = args!!.toMutableList()
            var path = section.name

            if(args.isEmpty()){
                section.command(sender!!)
                return true
            }
            var section = this.section
            val tree = getTree(section)
            //Start with the full argument list, and decrease by one until match is found in command section tree,
            //then update the arguments list to remove the path of the section that was found.
            for((x, _) in args.toMutableList().withIndex()){
                val node = args.subList(args.size - x - 1, args.size - 1).joinToString(separator = " ")
                if(tree.containsKey(section.name + " " + node)){
                    section = tree[section.name + " " + node]!!
                    path = node
                    args.removeAll(args.subList(0, x))
                    break
                }
            }

            val cmds = section::class.members.filter { it.findAnnotation<com.scarabcoder.commandapi2.Command>() != null }.associate { it.name to it }

            if(args.isEmpty()){
                section.command(sender!!)
                return true
            }
            var cmdFunc = cmds[args[0]]
            if(cmdFunc == null){
                if(args.size > 0 && args[0] == "help"){
                    try {
                        section.showHelp(sender!!, if(args.size == 2) args[1].toInt() else 1)
                    } catch (e: NumberFormatException){
                        sender!!.sendMessage("${ChatColor.RED}Enter a valid number for page!")
                    }
                    return true
                }
                for((name, func) in cmds){
                    if(func.findAnnotation<com.scarabcoder.commandapi2.Command>() != null){
                        val cmdAnn = func.findAnnotation<com.scarabcoder.commandapi2.Command>()!!
                        if(cmdAnn.aliases.isEmpty()) continue
                        if(cmdAnn.aliases.contains(args[0]))
                            cmdFunc = func
                    }
                }
            }
            args.removeAt(0) //Remove the function name, leaving just the arguments.
            if(cmdFunc == null){
                sender!!.sendMessage(Messages.commandNotFound.replace("\$c", getHelpCmd(section)))
                return true
            }

            if(cmdFunc.parameters.isEmpty()){
                sender!!.sendMessage(Messages.commandNotFound.replace("\$c", getHelpCmd(section)))
                return true
            }
            val senderType = cmdFunc.parameters[1].type.classifier as KClass<*>
            sender!!
            var senderObj: Any = sender
            if(!senderType.isInstance(sender)){
                if(senderType.isSubclassOf(ConsoleCommandSender::class)){
                    sender.sendMessage(Messages.consoleOnly)
                    return true
                }else if(senderType.isSubclassOf(Player::class)){
                    sender.sendMessage(Messages.playerOnly)
                    return true
                }else if(ArgumentParsers.supportsSender(senderType)){
                    try {
                        val parsed = ArgumentParsers.parseSender(sender, senderType)
                        senderObj = parsed
                    } catch(e: ArgumentParseException){
                        sender.sendMessage("${ChatColor.RED}Error: ${e.name}")
                        return true
                    }
                }else{
                    sender.sendMessage(Messages.commandNotFound.replace("\$c", getHelpCmd(section)))
                    return true
                }
            }
            val params = cmdFunc.parameters.toMutableList()
            params.removeAt(0) //Remove the instance as it isn't an argument
            params.removeAt(0) //Remove the sender as it isn't an argument


            val call: MutableMap<KParameter, Any?> = HashMap()
            for((i, param) in params.withIndex()){

                if(i == args.size){
                    sender.sendMessage(Messages.invalidArguments.replace("\$u", "/$path ${getCmdUsage(cmdFunc)}"))
                    return true
                }
                val argAnn = param.findAnnotation<Argument>()
                if(argAnn != null && argAnn.sentence){
                    val sent = args.subList(i, args.size)
                    if(sent.size == 0){
                        sender.sendMessage(Messages.invalidArguments.replace("\$u", "/$path ${getCmdUsage(cmdFunc)}"))
                        return true
                    }
                    call.put(param, args.subList(i, args.size).joinToString(separator = " "))
                    break
                }

                if(param.type.classifier!! == String::class){
                    call.put(param, args[i])
                    continue
                }
                try {
                    call.put(param, ArgumentParsers.parse(args[i], param.type.classifier as KClass<*>))
                } catch(e: ArgumentParseException){
                    sender.sendMessage("${ChatColor.RED}Error with argument ${getParamName(param)}: ${e.name}")
                    return true
                } catch (e: CommandException){
                    sender.sendMessage("${ChatColor.RED}There was an internal parsing error with argument ${getParamName(param)}: ${e.name}")
                    return true
                }
                //User sent more arguments than is required.
                if(i == params.size - 1 && args.size - 1 > i){
                    sender.sendMessage(Messages.invalidArguments.replace("\$u", "/$path ${getCmdUsage(cmdFunc)}"))
                    return true
                }
            }
            call.put(cmdFunc.parameters[0], section) //Set the object instance
            call.put(cmdFunc.parameters[1], senderObj) //Re-add the sender to the function param list.

            cmdFunc.callBy(call)

            return true
        }

    }

    fun getPage(pages: List<String>, page: Int): List<String> {
        val linesPerPage = 9
        val pageStart = 0 + (linesPerPage * page - 1)
        val pageEnd = pageStart + linesPerPage
        val maxPages = (Math.ceil(pages.size.toDouble() / linesPerPage.toDouble())).toInt()
        return pages
    }

    fun getHelpCmd(section: CommandSection): String {
        return "/" + if(section.parentPath == "") "" else section.parentPath + " " + section.name + " help [page]"
    }

    fun getHelpStrings(section: CommandSection, index: Int = 1): List<String> {
        val index = (index - 1).constrainMin(1)
        var linesPerPage = 9
        val usageTemplate = "${ChatColor.GOLD}%cmd%: ${ChatColor.WHITE}%description%"
        val lines = ArrayList<String>()
        val header = "${ChatColor.YELLOW}--------- ${ChatColor.WHITE}Help: ${section.readableName} ${ChatColor.GRAY}(%page%/%pages%) ${ChatColor.YELLOW}-----------------"
        if(section.description != ""){
            linesPerPage--
        }
        //lines.add(header)
        for(subSection in section.sections){
            lines.add(usageTemplate.replace("%cmd%", "/${subSection.value.parentPath} (${subSection.value.name}) ...").replace("%description%", subSection.value.description))
        }
        section::class.members.filter { it.findAnnotation<com.scarabcoder.commandapi2.Command>() != null }.forEach {
            val cmd = it.findAnnotation<com.scarabcoder.commandapi2.Command>()
            var path = section.parentPath
            if(path != "") path += " "
            val usage = getCmdUsage(it)
            lines.add(usageTemplate.replace("%cmd%", "/$path${section.name} ${usage.substring(0, usage.length - 1)}").replace("%description%", cmd!!.description))
        }
        if(index >= lines.size){

        }
        val maxPages = (Math.ceil((lines.size - 1).toDouble() / linesPerPage.toDouble())).toInt()
        val pageStart =  (linesPerPage * index - 1).constrain(0, maxPages)
        val pageEnd = (pageStart + linesPerPage).constrain(0, maxPages)
        val displayLines = lines.subList(pageStart, pageEnd)

        displayLines.add(0, header.replace("%page%", Ints.constrainToRange(index,1, maxPages.toInt()).toString()).replace("%pages%", maxPages.toInt().toString()))
        if(section.description != "")
            displayLines.add(1, "${ChatColor.GRAY}${section.description}")
        return displayLines
    }




    private fun getTree(section: CommandSection): HashMap<String, CommandSection> {
        val tree: HashMap<String, CommandSection> = HashMap()

        tree.put(section.name, section)
        for((name, sec) in section.getChildren()){
            tree.put("${section.name} $name", sec)
        }
        return tree
    }

}