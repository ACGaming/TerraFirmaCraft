#  Work under Copyright. Licensed under the EUPL.
#  See the project README.md and LICENSE.txt for more information.

# Script to run all resource generation

from mcresources import ResourceManager, clean_generated_resources

import assets
import data
import world_gen
import recipes
import ore_veins
import collapse_recipes

from constants import *


def main():
    rm = ResourceManager('tfc', resource_dir='../src/main/resources')
    clean_generated_resources('/'.join(rm.resource_dir))

    # do simple lang keys first, because it's ordered intentionally
    rm.lang(DEFAULT_LANG)

    # generic assets / data
    assets.generate(rm)
    data.generate(rm)
    world_gen.generate(rm)
    recipes.generate(rm)

    # more complex stuff n things
    ore_veins.generate(rm)
    collapse_recipes.generate(rm)

    # Random things
    rm.item_tag('forge:ingots/cast_iron', 'minecraft:iron_ingot')
    rm.block_tag('tree_grows_on', 'minecraft:grass_block', '#forge:dirt', '#tfc:grass')
    rm.block_tag('bush_plantable_on', 'minecraft:grass_block', '#forge:dirt', '#tfc:grass')

    rm.flush()


if __name__ == '__main__':
    main()
