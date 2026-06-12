package uk.foodclassifier.text



import uk.foodclassifier.normalizeBrackets

import kotlin.math.max

import kotlin.math.min



internal object LabelBoundaryFinder {

    private val ingredientsHeaders = listOf(

        Regex("""(?i)(?:^|[\n\r])\s*Ingredients\s*:\s*"""),

        Regex("""(?i)\bIngredients\s*:\s*"""),

        Regex("""(?i)\bEN-INGREDIENTS?\s*:?\s*"""),

        Regex("""(?i)\bNGREDIENTS?\s*:?\s*"""),

        Regex("""(?i)\b[\w-]{0,12}gredients\s*:\s*"""),

        Regex("""(?i)\bngredients\s*:\s*"""),

        Regex("""(?i)\b1ngredients\s*:\s*"""),

        Regex("""(?i)(?:^|[\n\r])\s*oredients\s*:\s*"""),

        Regex("""(?i)\boredients\s*:\s*"""),

        Regex("""(?i)(?:^|[\n\r])\s*lngredients\s*:\s*"""),

        Regex("""(?i)\bComposition\s*:\s*"""),

        Regex("""(?i)\bINCI\s*:\s*"""),

    )



    private val explicitIngredientsSection = Regex(

        """(?is)\bingredients\s*:\s*(.+?)(?=\bfor\s+allergens\b|\ballergen\s+information\b|\ballergen\b|\bsuitable\s+for\b|\bkeep\s+refrigerated\b|\bnutrition|\ballergy|\bstorage\b|\bmay\s+contain\b|\bimportant\s+usage\b|\busage\s+instructions\b|\bart[.\-]?\s*no\b|\bmade\s+in\b|\bwww\.|\bcontact\b|\bbest\s+before\b|\buse\s+by\b|$)""",

    )



    private val headerFalseStarts = listOf(

        "in bold",

        "in capital",

        "listed",

        "list ",

        "above",

        "below",

        "for allergen",

        "for allerg",

    )



    private val sectionEndPatterns = AllergyBoundary.sectionEndPatterns + listOf(

        Regex("""(?i)\bincluding\s+cereals\b"""),

        Regex("""(?i)\ballergen\s+information\b"""),

        Regex("""(?i)\ballergen\s+adv[i1l]ce\b"""),

        Regex("""(?i)\bfor\s+allergens\b"""),

        Regex("""(?i)\ballergens?\s*:"""),

        Regex("""(?i)\bmay\s+contain\b"""),

        Regex("""(?i)\bdietary\s+advice\b"""),

        Regex("""(?i)\bsuitable\s+for\b"""),

        Regex("""(?i)\bkeep\s+refrigerated\b"""),

        Regex("""(?i)\bnutrition(?:al)?\s+information\b"""),

        Regex("""(?i)\bnutritional\s+values\b"""),

        Regex("""(?i)\bstorage\b"""),

        Regex("""(?i)\bimportant\s+usage\b"""),

        Regex("""(?i)\busage\s+instructions\b"""),

        Regex("""(?i)\bcontact\b"""),

        Regex("""(?i)\b00800\b"""),

        Regex("""(?i)\bbest\s+before\b"""),

        Regex("""(?i)\buse\s+by\b"""),

        Regex("""(?i)\bpacked\s+in\b"""),

        Regex("""(?i)\bart[.\-]?\s*no\b"""),

        Regex("""(?i)\bmade\s+in\b"""),

        Regex("""(?i)\bwww\."""),

        Regex("""\s\d\s+\d{6,}"""),

        Regex("""\b\d{8,}\b"""),

        Regex("""(?i)\b\d+\s*ml\s*e\b"""),

    )



    private val afterAllergyBoilerplate = Regex(

        """(?i)(?:for\s+allergens[^.]*\.|see\s+ingredients\s+in\s+bold\.?)\s*(.+)""",

    )



    fun findBlock(rawOcr: String): String? {

        val text = normalizeInput(rawOcr)

        if (text.isBlank()) {

            return null

        }



        extractExplicitIngredientsSection(text)?.let { return it }



        val headerMatch = findBestHeader(text)

        if (headerMatch != null) {

            val start = headerMatch.range.last + 1

            val end = endIndex(text, start) ?: text.length

            if (start < end) {

                val block = text.substring(start, end).trim()

                if (isUsefulIngredientBlock(block)) {

                    return block

                }

            }

        }



        extractAfterAllergyBoilerplate(text)?.let { return it }



        findBestIngredientSpan(text)?.let { return it }



        val sectionEnd = endIndex(text, startIndex = 0)

        if (sectionEnd != null && sectionEnd > 0) {

            val candidate = text.substring(0, sectionEnd)

            if (isUsefulIngredientBlock(candidate)) {

                return candidate

            }

        }



        return null

    }



    private fun extractExplicitIngredientsSection(text: String): String? {

        return explicitIngredientsSection.findAll(text)

            .mapNotNull { match ->

                match.groupValues[1].trim().trimEnd('.', ';', ':')

            }

            .filter { isUsefulIngredientBlock(it) }

            .maxByOrNull { it.length }

    }



    private fun extractAfterAllergyBoilerplate(text: String): String? {

        return afterAllergyBoilerplate.findAll(text)

            .map { match -> trimLineAtSectionEnd(match.groupValues[1].trim()) }

            .filter { isUsefulIngredientBlock(it) }

            .maxByOrNull { it.count { char -> char == ',' } }

    }



    private fun findBestIngredientSpan(text: String): String? {

        return text.split(Regex("""[\n\r]+"""))

            .map { line -> trimLineAtSectionEnd(line.trim()) }

            .filter { isUsefulIngredientBlock(it) && !isSimpleLowercaseList(it) }

            .maxByOrNull { it.count { char -> char == ',' } }

    }



    private fun trimLineAtSectionEnd(line: String): String {

        val end = endIndex(line, 0) ?: line.length

        return line.substring(0, end).trim().trimEnd('.', ';')

    }



    private fun isUsefulIngredientBlock(block: String): Boolean {

        if (block.isBlank()) {

            return false

        }

        val lower = block.lowercase()

        if (

            lower.startsWith("for allergen") ||

            lower.startsWith("see ingredient") ||

            lower.startsWith("suitable for") ||

            lower.startsWith("keep refrigerated")

        ) {

            return false

        }

        return looksLikeIngredientList(block) ||

            block.count { it == ',' } >= 2 ||

            hasMultipleIngredientLines(block)

    }



    private fun hasMultipleIngredientLines(block: String): Boolean {

        val lines = block.split(Regex("""[\n\r]+"""))

            .map { it.trim().trimEnd(',', '.', ';') }

            .filter { it.isNotBlank() }

        if (lines.size < 2) {

            return false

        }

        return lines.count { line ->

            val lower = line.lowercase()

            line.length >= 3 &&

                !lower.startsWith("for allergen") &&

                !lower.startsWith("see ingredient") &&

                !lower.startsWith("ingredients") &&

                !lower.startsWith("suitable for")

        } >= 2

    }



    private fun isSimpleLowercaseList(block: String): Boolean {

        val items = block.split(',').map { it.trim() }

        if (items.size < 2) {

            return false

        }

        return items.all { item ->

            item.isNotEmpty() && item.all { char ->

                char.isLowerCase() || char.isWhitespace() || char == '-'

            }

        }

    }



    private fun normalizeInput(rawOcr: String): String {

        return normalizeBrackets(

            rawOcr

                .replace('\r', '\n')

                .replace(Regex("""([A-Za-z])-\s+([A-Za-z])""")) { match ->

                    "${match.groupValues[1]}${match.groupValues[2]}"

                },

        )

    }



    private fun findBestHeader(text: String): MatchResult? {

        return ingredientsHeaders

            .flatMap { pattern -> pattern.findAll(text) }

            .filter { match -> isValidHeaderMatch(text, match) }

            .minByOrNull { it.range.first }

    }



    private fun isValidHeaderMatch(text: String, match: MatchResult): Boolean {

        val before = text.substring(max(0, match.range.first - 24), match.range.first).lowercase()

        if (before.contains("see ") || before.contains("for allergen")) {

            return false

        }



        val after = text.substring(

            match.range.last + 1,

            min(text.length, match.range.last + 40),

        ).trimStart().lowercase()



        if (headerFalseStarts.any { after.startsWith(it) }) {

            return false

        }



        return true

    }



    private fun endIndex(text: String, startIndex: Int): Int? {

        if (startIndex >= text.length) {

            return null

        }

        return sectionEndPatterns

            .mapNotNull { pattern -> pattern.find(text, startIndex)?.range?.first }

            .minOrNull()

    }



    private fun looksLikeIngredientList(text: String): Boolean {

        return text.contains(',') || text.contains('(') || text.contains('[')

    }

}


