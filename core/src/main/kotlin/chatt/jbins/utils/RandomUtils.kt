package chatt.jbins.utils

import java.util.*

fun uuid(): String = UUID.randomUUID().toString().replace("-","")