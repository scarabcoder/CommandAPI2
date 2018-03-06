package com.scarabcoder.commandapi2

import com.google.common.primitives.Ints
import com.scarabcoder.commandapi2.CommandRegistry.getCmdUsage
import com.scarabcoder.commandapi2.CommandRegistry.getParamName
import com.scarabcoder.commandapi2.exception.ArgumentParseException
import com.scarabcoder.commandapi2.exception.ArgumentTypesException
import com.scarabcoder.commandapi2.exception.CommandException
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import org.bukkit.util.StringUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.isSubclassOf
import com.scarabcoder.commandapi2.Command as CmdAnn


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

    class RootCommandHandler(val section: CommandSection): Command(section.name, section.description, section.usage, section.aliases){

        override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): MutableList<String> {
            val modifiedArgs = args.toMutableList()
            var section = section
            var possibilities: MutableList<String> = ArrayList()
            val tree = getTree(section)
            for((x, _) in args.toMutableList().withIndex()){
                val node = modifiedArgs.subList(0, args.size - x).joinToString(separator = " ")
                if(tree.containsKey(section.name + " " + node)){
                    section = tree[section.name + " " + node]!!
                    modifiedArgs.removeAll(modifiedArgs.subList(0, x))
                    break
                }
            }
            val cmdFunc = section::class.findMember(modifiedArgs.first())
            if(cmdFunc != null && cmdFunc.hasAnnotation<CmdAnn>()){
                return Collections.emptyList()
            }

            for(subSection in section.sections){
                possibilities.add(subSection.key)
            }
            section::class.members.filter { it.findAnnotation<CmdAnn>() != null }
                    .forEach { possibilities.add(it.name) }
            possibilities = possibilities.filter { it.startsWith(modifiedArgs.last(), ignoreCase = true) }.toMutableList()
            return ArrayList(HashSet<String>(possibilities))
        }


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
                val node = args.subList(0, args.size - x).joinToString(separator = " ")
                if(tree.containsKey(section.name + " " + node)){
                    section = tree[section.name + " " + node]!!
                    path = node
                    args.removeAll(args.subList(0, x))
                    break
                }
            }

            val cmds = section::class.members.filter { it.hasAnnotation<CmdAnn>() }.associate { it.name to it }

            if(args.isEmpty()){
                section.command(sender!!)
                return true
            }
            //region Find the function using the first argument name as the key.
            //Find the function by the argument name
            var cmdFunc = cmds[args[0]]
            //If it wasn't found, check for aliases. If none exist, check for the hardcoded help command and send the help usages.
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
                    if(func.findAnnotation<CmdAnn>() != null){
                        val cmdAnn = func.findAnnotation<CmdAnn>()!!
                        if(cmdAnn.aliases.isEmpty()) continue
                        if(cmdAnn.aliases.contains(args[0])){
                            cmdFunc = func
                        }
                    }
                }
            }
            args.removeAt(0) //Remove the function name, leaving just the arguments.
            //endregion

            //region Validate that the function exists and that it's setup somewhat correctly.
            if(cmdFunc == null || cmdFunc.parameters.isEmpty()){
                sender!!.sendMessage(Messages.commandNotFound.replace("\$c", section.helpCmd))
                return true
            }
            //endregion
            val cmdAnn = cmdFunc.findAnnotation<CmdAnn>()!!

            //region Check sender type and cast if needed/possible.
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
                    sender.sendMessage(Messages.commandNotFound.replace("\$c", section.helpCmd))
                    return true
                }
            }
            //endregion

            //region Check sender permissions
            if(!cmdAnn.noPerms){
                val perm = if(cmdAnn.permission.isBlank()) getRootPermission(section) + ".${cmdFunc.name}" else cmdAnn.permission
                if(!sender.hasPermission(perm)){
                    sender.sendMessage(Messages.noPermission)
                    return true
                }
            }
            //endregion

            //region Apply validators and check result
            if(!cmdAnn.validators.isEmpty()){
                for(validatorID in cmdAnn.validators){
                    val valid = CommandValidator.getValidator(validatorID)!!.validate(sender)
                    valid?.let {
                        sender.sendMessage(valid)
                        return true
                    }
                }
            }
            //endregion

            //region Check and cast actual parameters
            val params = cmdFunc.parameters.toMutableList()
            params.removeAt(0) //Remove the instance as it isn't an argument
            params.removeAt(0) //Remove the sender as it isn't an argument


            val call: MutableMap<KParameter, Any?> = HashMap()

            try {
                call.putAll(handleArgTyping(cmdFunc, args))
            } catch (e: ArgumentTypesException){
                when (e.reason) {
                    ArgumentTypesException.Reason.INVALID_USAGE ->
                        sender.sendMessage(Messages.invalidArguments.replace("\$u", "/$path ${getCmdUsage(cmdFunc)}"))
                    else -> sender.sendMessage(e.msg)
                }
                return true
            }
            //endregion
            call.put(cmdFunc.parameters[0], section) //Set the object instance
            call.put(cmdFunc.parameters[1], senderObj) //Re-add the sender to the function param list.

            cmdFunc.callBy(call)

            return true
        }

    }

    internal class SingleMultiCmdHandler(val function: KCallable<*>, val obj: Any): Command(function.name, "", "", Collections.emptyList()) {

        private val cmdAnn = function.findAnnotation<CmdAnn>()!!

        override fun getDescription(): String = cmdAnn.description
        override fun getAliases(): MutableList<String> = cmdAnn.aliases.toMutableList()
        override fun getUsage(): String = getCmdUsage(function)

        override fun execute(sender: CommandSender, label: String, args: Array<out String>): Boolean {

            val argTypes = function.parameters.toMutableList()
            argTypes.removeAt(0) //Remove object instance, not an argument
            argTypes.removeAt(0) //Remove sender parameter, not an argument
            val toPass = HashMap<KParameter, Any>()

            val senderType = function.parameters[1].type.classifier as KClass<*>
            var senderObj: Any = sender
            if(!senderType.isInstance(sender)){
                when {
                    senderType.isSubclassOf(ConsoleCommandSender::class) -> {
                        sender.sendMessage(Messages.consoleOnly)
                        return true
                    }
                    senderType.isSubclassOf(Player::class) -> {
                        sender.sendMessage(Messages.playerOnly)
                        return true
                    }
                    ArgumentParsers.supportsSender(senderType) -> try {
                        val parsed = ArgumentParsers.parseSender(sender, senderType)
                        senderObj = parsed
                    } catch(e: ArgumentParseException){
                        sender.sendMessage("${ChatColor.RED}Error: ${e.name}")
                        return true
                    }
                    else -> {
                        sender.sendMessage(Messages.consoleOnly)
                        return true
                    }
                }

            }
            toPass.put(function.parameters[0], obj)
            toPass.put(function.parameters[1], senderObj)

            try {
                toPass.putAll(handleArgTyping(function, args.toMutableList()))
            } catch (e: ArgumentTypesException){
                when (e.reason) {
                    ArgumentTypesException.Reason.INVALID_USAGE ->
                        sender.sendMessage(Messages.invalidArguments.replace("\$u", "/${getCmdUsage(function)}"))
                    else -> sender.sendMessage(e.msg)
                }
                return true
            }

            function.callBy(toPass)

            return true
        }

    }

    private fun getRootPermission(section: CommandSection): String = section.fullPath.replace(" ", ".")

    private fun handleArgTyping(function: KCallable<*>, args: MutableList<String>): Map<KParameter, Any> {
        val toPass = HashMap<KParameter, Any>()
        val params = function.parameters.toMutableList()
        params.removeAt(0)
        params.removeAt(0)
        for((i, param) in params.withIndex()){

            if(i >= args.size && !param.isOptional){
                throw ArgumentTypesException(ArgumentTypesException.Reason.INVALID_USAGE)
            }
            if(param.isOptional) break
            val argAnn = param.findAnnotation<Argument>()
            if(argAnn != null && argAnn.sentence){
                val sent = args.subList(i, args.size)
                if(sent.size == 0){
                    throw ArgumentTypesException(ArgumentTypesException.Reason.INVALID_USAGE)
                }
                toPass.put(param, args.subList(i, args.size).joinToString(separator = " "))
                break
            }

            if(param.type.classifier!! == String::class){
                toPass.put(param, args[i])
                continue
            }
            try {
                toPass.put(param, ArgumentParsers.parse(args[i], param.type.classifier as KClass<*>)!!)
            } catch(e: ArgumentParseException){
                throw ArgumentTypesException(ArgumentTypesException.Reason.ARG_PROCESS_ERROR, "${ChatColor.RED}Error with argument ${getParamName(param)}: ${e.name}")
            } catch (e: CommandException){
                throw ArgumentTypesException(ArgumentTypesException.Reason.INTERNAL_ERROR, "${ChatColor.RED}There was an internal parsing error with argument ${getParamName(param)}: ${e.name}")
            }
            //User sent more arguments than is required.
            if(i == params.size - 1 && args.size - 1 > i){
                throw ArgumentTypesException(ArgumentTypesException.Reason.INVALID_USAGE)
            }
        }

        return toPass
    }

    fun getPage(pages: List<String>, page: Int): List<String> {
        val linesPerPage = 9
        val pageStart = 0 + (linesPerPage * page - 1)
        val pageEnd = pageStart + linesPerPage
        val maxPages = (Math.ceil(pages.size.toDouble() / linesPerPage.toDouble())).toInt()
        return pages
    }

    fun getHelpStrings(section: CommandSection, index: Int = 1): List<String> {
        val index = index.constrainMin(1)
        var linesPerPage = 9
        val usageTemplate = "${ChatColor.GOLD}%cmd%: ${ChatColor.WHITE}%description%"
        val lines = ArrayList<String>()
        val header = "${ChatColor.YELLOW}--------- ${ChatColor.WHITE}Help: ${section.readableName} ${ChatColor.GRAY}(%page%/%pages%) ${ChatColor.YELLOW}-----------------"
        if(section.description != ""){
            linesPerPage--
        }
        //lines.add(header)
        for(subSection in section.sections){
            val pp = if(subSection.value.parentPath == "") " " else subSection.value.parentPath
            lines.add(usageTemplate.replace("%cmd%", "/$pp ${subSection.value.name} ...").replace("%description%", subSection.value.description))
        }
        section::class.members.filter { it.findAnnotation<CmdAnn>() != null }.forEach {
            val cmd = it.findAnnotation<CmdAnn>()
            var path = section.parentPath
            if(path != "") path += " "
            val usage = getCmdUsage(it)
            lines.add(usageTemplate.replace("%cmd%", "/$path${section.name} ${usage.substring(0, usage.length - 1)}").replace("%description%", cmd!!.description))
        }
        if(index >= lines.size){

        }
        val maxPages = (Math.ceil((lines.size - 1).constrainMin(1).toDouble() / linesPerPage.toDouble())).toInt()
        val pageStart =  (linesPerPage * (index - 1)).constrain(0, lines.size - 1)
        val pageEnd = (pageStart + linesPerPage).constrain(0, lines.size)
        val displayLines = lines.subList(pageStart, pageEnd)

        displayLines.add(0, header.replace("%page%", Ints.constrainToRange(index,1, maxPages).toString()).replace("%pages%", maxPages.toInt().toString()))
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