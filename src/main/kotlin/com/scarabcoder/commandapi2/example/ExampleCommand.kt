package com.scarabcoder.commandapi2.example

import com.scarabcoder.commandapi2.Argument
import com.scarabcoder.commandapi2.Command
import com.scarabcoder.commandapi2.CommandSection
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*


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
class ExampleCommand(name: String): CommandSection(name) {

    init {
        section(ExampleSubSection())
    }

    /**
     * Function when the command is sent without arguments.
     */
    override fun command(sender: CommandSender){

    }

    /**
     * Example of usage:
     *       /example example arg
     *       /example test arg
     */
    @Command(aliases = ["test"], description = "An example command.")
    fun example(sender: Player, @Argument(name = "Ex. Arg") exArg: String) {
        sender.sendMessage("Example command with argument: $exArg")
    }

    @Command
    fun sender(sender: UUID){
        Bukkit.getPlayer(sender).sendMessage("Example of custom sender type (UUID instead of Player): $sender")
    }

    /**
     * Example of usage:
     *      /sentence Hello World
     */
    @Command(aliases = ["sen"], description = "Example of multi-word arguments")
    fun sentence(sender: Player, @Argument(sentence = true) sentence: String){
        sender.sendMessage("Example of a multi-word argument: $sentence")
    }

    @Command
    fun heal(sender: Player, toHeal: Player = sender){
        sender.sendMessage("${ChatColor.GREEN}Healed ${toHeal.name}")
        toHeal.health = 20.0

    }

    /**
     * Example of usage:
     *      /player ScarabCoder
     */
    @Command(aliases = ["pl"], description = "Casting to objects using custom or default casters")
    fun player(sender: Player, player: Player){
        sender.sendMessage("Example of type arguments using type converters: ${player.name}")
    }

    @Command(description = "Create a team with the given name.")
    fun create(sender: Player, @Argument(name = "Team Name") teamName: String){
        //Create a team with the teamName parameter
    }

}