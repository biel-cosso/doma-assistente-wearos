package com.example.domaassistente

// item da lista de avisos
data class Aviso(val tipo: Tipo, val texto: String) {

    enum class Tipo(val rotulo: String, val icone: String) {
        MENSAGEM("Mensagem", "✉"),
        LEMBRETE("Lembrete", "⏰"),
        TREINAMENTO("Treinamento", "🎓"),
        ALERTA("Alerta de segurança", "⚠"),
    }

    // texto que aparece na lista
    override fun toString() = "${tipo.icone} ${tipo.rotulo}\n$texto"

    // texto que é falado
    fun paraFala() = "${tipo.rotulo}. $texto"

    companion object {
        // avisos de exemplo (a empresa Doma é fictícia)
        fun exemplos() = listOf(
            Aviso(Tipo.ALERTA, "Simulação de evacuação hoje às 16 horas. Siga a sinalização até o ponto de encontro."),
            Aviso(Tipo.MENSAGEM, "Reunião geral às 15 horas no auditório."),
            Aviso(Tipo.LEMBRETE, "Pausa para alongamento em 10 minutos."),
            Aviso(Tipo.ALERTA, "Alerta de tempestade. Evite as áreas externas."),
            Aviso(Tipo.TREINAMENTO, "Dica: diga \"ler alertas\" para ouvir só os alertas de segurança."),
            Aviso(Tipo.MENSAGEM, "O refeitório funciona até as 14 horas hoje."),
        )
    }
}
