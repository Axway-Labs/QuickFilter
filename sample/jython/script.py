from com.vordel.trace import Trace

def attach(ctx, entity):
    Trace.info("Jython True Filter attached")

def detach():
    Trace.info("Jython True Filter detached")

def invoke(circuit, msg):
    Trace.info("Jython True Filter called")
    return True
