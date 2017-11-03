package nl.reikrul.kosote

class Project(val name: String) {

    private val _testCases: MutableList<TestCase> = mutableListOf()
    val testCases: List<TestCase>
        get() = _testCases.toList()

    fun test(name: String): TestCase =
            if (_testCases.any { it.name == name }) {
                error("TestCase already exists: $name")
            } else {
                TestCase(name).also { _testCases.add(it) }
            }
}