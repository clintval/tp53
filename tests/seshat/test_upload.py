from tp53.seshat import HumanGenomeAssembly


def test_human_genome_assembly() -> None:
    """Test that the human genome assembly is string-like."""
    assert HumanGenomeAssembly.hg18 == "hg18"
    assert HumanGenomeAssembly.hg19 == "hg19"
    assert HumanGenomeAssembly.hg38 == "hg38"
