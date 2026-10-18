package com.aistudy.myapplication.control

sealed interface ControlEvent {
    data class Press(val id:Int,val button:ControlButton):ControlEvent
    data class Release(val id:Int):ControlEvent
    data class Leave(val id:Int):ControlEvent
    data class Joystick(val id:Int,val x:Float,val y:Float):ControlEvent
    data class Limiter(val percent:Int):ControlEvent
    data class Mode(val mode:DriveMode):ControlEvent
    data object Kill:ControlEvent
    data object Cancel:ControlEvent
    data object Turn:ControlEvent
    data object BeginUnlock:ControlEvent
    data object EndUnlock:ControlEvent
}
fun ControlReducer.handle(event:ControlEvent):Boolean {
    val directionStop=when(event){is ControlEvent.Release->isDirectionPointerValid(event.id);is ControlEvent.Leave->isDirectionPointerValid(event.id);else->false}
    val wasBrake=state.brake
    when(event) {
        is ControlEvent.Press->press(event.id,event.button)
        is ControlEvent.Release->release(event.id)
        is ControlEvent.Leave->leaveControl(event.id)
        is ControlEvent.Joystick->moveJoystick(event.id,event.x,event.y)
        is ControlEvent.Limiter->setLimiter(event.percent)
        is ControlEvent.Mode->requestMode(event.mode)
        ControlEvent.Kill->kill()
        ControlEvent.Cancel->cancelAll()
        ControlEvent.Turn->quickTurn()
        ControlEvent.BeginUnlock->beginUnlock()
        ControlEvent.EndUnlock->cancelAll()
    }
    return directionStop || event==ControlEvent.Kill || event==ControlEvent.Cancel || wasBrake!=state.brake
}
