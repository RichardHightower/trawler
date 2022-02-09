package trawler.core.internal.util

import java.io.File


data class RegexRuleResult(val success: Boolean, val message: String? = null) {
    fun matched() = success
    fun failedMatch() = !success
}
data class RegexRule(val regex: Regex, val message:String) {
    fun isMatch( value:String) : Boolean = regex.matchEntire(value) !=null
    fun matchValue(value: String, name: String? = null, context: Any? = null,  file: File?=null) : RegexRuleResult =
        if (isMatch(value)) {
            RegexRuleResult(true)
        } else {
            if (name != null && context != null) {
                RegexRuleResult(false, "$name: $message \n $context \n $file")
            } else if (name !=null) {
                RegexRuleResult(false, "$name: $message $file")
            } else {
                RegexRuleResult(false, message)
            }
        }

}

enum class ResponseMessageType {
    WARNING,
    ERROR,
    FATAL
}

data class ResponseMessage (val message: String, val type: ResponseMessageType, val causedBy : List<ResponseMessage> = listOf())

data class Result<T>( val success:Boolean, val value: T?, val responseMessage: ResponseMessage?=null) {
   fun isFailure() : Boolean = !success
   fun result() : T = this.value!!
   fun message() : ResponseMessage = this.responseMessage!!

   fun isWarning()  = if (responseMessage!=null) responseMessage.type === ResponseMessageType.WARNING  else false
   fun isError()  = if (responseMessage!=null) responseMessage.type === ResponseMessageType.ERROR  else false
   fun isFatal()  = if (responseMessage!=null) responseMessage.type === ResponseMessageType.FATAL  else false
   fun isAnyErrorType() : Boolean  =  isWarning() || isError() || isFatal()
}

class Results <T> {
    fun result(result: T) = Result( success = true, value = result)
    fun resultWithWarning(result: T, message:String, causedBy : List<ResponseMessage> = listOf()) = Result( success = true, value = result, ResponseMessage(message, ResponseMessageType.WARNING, causedBy))
    fun error(message:String) = Result<T>( success = false, null, ResponseMessage(message, ResponseMessageType.ERROR))
    fun errors(message:String, causedBy : List<ResponseMessage>): Result<T> {
        val messageType = if (causedBy.any { it.type == ResponseMessageType.FATAL }) {
            ResponseMessageType.FATAL
        } else if (causedBy.any { it.type == ResponseMessageType.ERROR }) {
            ResponseMessageType.ERROR
        } else {
            ResponseMessageType.WARNING
        }
        return Result(success = false, null, ResponseMessage(message, messageType, causedBy))
    }
    fun warning(message:String) = Result<T>( success = true, null, ResponseMessage(message, ResponseMessageType.WARNING))
    fun fatal(message:String) = Result<T>( success = false, null, ResponseMessage(message, ResponseMessageType.FATAL))
}

