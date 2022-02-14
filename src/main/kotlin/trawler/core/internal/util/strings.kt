package trawler.core.internal.util



fun String.camelToSqlName(): String {
    val result = StringBuilder().append(Character.toUpperCase(this[0]))
    for (i in 1 until this.length) {
        val ch = this[i]
        if (Character.isUpperCase(ch)) {
            result.append('_')
            result.append(Character.toUpperCase(ch))
        } else {
            result.append(Character.toUpperCase(ch))
        }
    }
    return result.toString()
}

fun String.sqlNameToCamel(upper:Boolean): String {
    val result = StringBuilder()
    if (upper) {
        result.append( Character.toUpperCase(this[0]))
    } else {
        result.append( Character.toLowerCase(this[0]))
    }
    var useUpper = false
    for (i in 1 until this.length) {
        val ch = this[i]
        if (ch == '_') {
            useUpper = true
            continue
        } else {
            if (useUpper) {
                result.append(Character.toUpperCase(ch))
            } else {
                result.append(Character.toLowerCase(ch))
            }
            useUpper = false
        }
    }
    return result.toString()
}

fun String.sqlNameToCamelUpper(): String  {
    return this.sqlNameToCamel(true)
}

fun String.sqlNameToCamelLower(): String  {
    return this.sqlNameToCamel(false)
}
