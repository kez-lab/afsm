package afsm.core

public data class AfsmGraphEntry(
    public val id: String,
    public val fileName: String,
    public val createTopology: () -> AfsmTopology,
)

public interface AfsmGraphRegistry {
    public val entries: List<AfsmGraphEntry>
}

