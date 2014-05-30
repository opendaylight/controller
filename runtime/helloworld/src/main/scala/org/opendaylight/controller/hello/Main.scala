package org.opendaylight.controller.hello

import scala.io.Source
import java.io.BufferedReader
import java.io.FileReader
import java.io.InputStreamReader

object Main extends App{
	//val lines = Source.fromFile("../resources/hello.txt").mkString
	//val text = io.Source.fromInputStream(getClass.getResourceAsStream("./src/main/resources/hello.txt")).mkString
	//println(text)
    //val text = new BufferedReader(new FileReader("resources/hello.txt")).readLine()
    val text = this.getClass().getClassLoader()
                                .getResourceAsStream("hello.txt")
                
    val text2 = new BufferedReader(new InputStreamReader(text)).readLine()
    println(text2)
}