import uk.foodclassifier.*

val list = """
Aqua,
Isopropyl Palmitate,
C12-15 Alkyl Benzoate,
Gy cerin,
Alcohol Denat,
Bis-Ethylhexyloxyphenol Methoxyphenyl Triozine,
Butyl Methoxydibenzoyimethane,
Dibutyl Adipate,
Ethylhexyl Trigzone,
Copernicia Cerifera Cera,
Glycerol Stearate,
Microcrysta" Wine Cellulose,
Phenylbenzimidazole Sulfonic Acid,
Sodium Ascorbyl Phosphate,
Sodium Hyaluronate,
Tocopheryl Acetate,
Xanthan Gum,
Polyglyceryl-3 Methylglucose Distearate,
Disostearoyl Polyglyceryl-a Dimer Dilinoleate,
Hydrogenated Ropeseed Oil,
Cetyl Paimitate,
Cellulose Gum,
Sodium Cetearyl Sulfate,
Sodium Hydroxide,
Trisodium Ethylenediamine Disuccinate,
Hydroxyacetophenone,
Ethylhexyglycerin,
Phenoxyethanol,
Linalool,
Benzyl Alcohol,
Linalyl Acetate,
Alpha-Isomethyl,
Citronelol,
Lanolin,
Parfum,
""".trimIndent()

val chips = parseIngredientsToChips(list)
println("Chips: ${chips.size}")
chips.forEach { println("  - $it") }

val result = classifyIngredients(formatIngredientsFromChips(chips))
println("\nClassification: ${result.classification}")
println("Definitive: ${result.definitiveClassification}")
println("NonVeg: ${result.matchedNonVegetarian}")
println("NonVegan: ${result.matchedNonVegan}")
println("Warnings: ${result.warnings.map { it.term + " in " + it.ingredient }}")
println("\nMessages:")
describeClassificationMatches(result.matchedNonVegetarian, result.matchedNonVegan).forEach { println("  $it") }
