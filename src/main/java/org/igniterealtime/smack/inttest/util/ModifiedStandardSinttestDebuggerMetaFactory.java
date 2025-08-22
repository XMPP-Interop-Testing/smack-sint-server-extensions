package org.igniterealtime.smack.inttest.util;

import org.igniterealtime.smack.inttest.debugger.SinttesDebuggerFactory;
import org.igniterealtime.smack.inttest.debugger.SinttesDebuggerMetaFactory;
import org.igniterealtime.smack.inttest.debugger.SinttestDebugger;

import java.time.ZonedDateTime;

public class ModifiedStandardSinttestDebuggerMetaFactory implements SinttesDebuggerMetaFactory
{
    @Override
    public SinttesDebuggerFactory create(String debuggerOptions)
    {
        return new ModifiedStandardSintDebuggerFactory(debuggerOptions);
    }

    class ModifiedStandardSintDebuggerFactory implements SinttesDebuggerFactory
    {
        final String debuggerOptions;

        public ModifiedStandardSintDebuggerFactory(String debuggerOptions)
        {
            this.debuggerOptions = debuggerOptions;
        }

        @Override
        public SinttestDebugger create(ZonedDateTime testRunStart, String testRunId)
        {
            return new ModifiedStandardSinttestDebugger(testRunStart, testRunId, debuggerOptions);
        }
    }
}
