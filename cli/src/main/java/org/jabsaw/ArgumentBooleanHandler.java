package org.jabsaw;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.args4j.*;
import org.kohsuke.args4j.spi.*;

/**
 * Boolean {@link OptionHandler}.
 *
 * @author Kohsuke Kawaguchi
 */
public class ArgumentBooleanHandler extends OptionHandler<Boolean> {
	private static final List<String> ACCEPTABLE_VALUES = Arrays
			.asList(new String[] { "true", "on", "yes", "1", "false", "off",
					"no", "0" });

	public ArgumentBooleanHandler(CmdLineParser parser, OptionDef option,
			Setter<? super Boolean> setter) {
		super(parser, option, setter);
	}

	@Override
	public int parseArguments(Parameters params) throws CmdLineException {
		if (params.size() > 0) {
			String valueStr = params.getParameter(0).toLowerCase();
			int index = ACCEPTABLE_VALUES.indexOf(valueStr);
			if (index != -1) {
				setter.addValue(index < ACCEPTABLE_VALUES.size() / 2);
				return 1;
			}
		}
		setter.addValue(true);
		return 0;
	}

	@Override
	public String getDefaultMetaVariable() {
		return null;
	}
}
