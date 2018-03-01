package com.scarabcoder.commandapi2.example

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

    }

    /**
     * Function when the command is sent without arguments.
     */
    override fun command(sender: Player){

    }

    /**
     * Example of usage:
     *       /example test
     */
    @Command(aliases = Arrays.asList("ex", "exmp"), description = "An example command.")
    fun example(sender: Player, @Argument(name = "Ex. Arg") exArg: String) {

    }

    /**
     * Example of usage:
     *      /sentence This is my sentence.
     */
    @Command(aliases = Arrays.asList("sen"), description = "Example of multi-word sentences")
    fun sentence(sender: Player, @Argument(vararg = true) sentence: String){

    }

    /**
     * Example of usage:
     *      /player ScarabCoder
     */
    @Command(aliases = "pl", description = "Casting to objects using custom or default casters")
    fun player(sender: Player, player: Player){

    }

}