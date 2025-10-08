package betterquesting.loaders.dsl;

import net.minecraft.util.ResourceLocation;

import betterquesting.api.properties.IPropertyType;
import betterquesting.api.properties.basic.PropertyTypeBoolean;

public class DslProps {

    public static final IPropertyType<Boolean> DSL_SOURCE = new PropertyTypeBoolean(
        new ResourceLocation("betterquesting:dsl_source"),
        false);
}
