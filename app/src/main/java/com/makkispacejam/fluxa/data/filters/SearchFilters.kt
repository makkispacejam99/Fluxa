package com.makkispacejam.fluxa.data.filters

object SearchFilters {

    // Bloques de entretenimiento (ES)
    val ENTERTAINMENT_BLOCKS_ES = listOf(
        "#videojuegos (secretos ocultos videojuegos|detalles insolitos de juegos|curiosidades easter eggs|analisis de juegos indie|iceberg de videojuegos|tops videojuegos|recomendaciones juegos|juegos indie ocultos|secretos videojuegos famosos)",
        "#baloncesto (jugadas increibles baloncesto|mejores fintas baloncesto|consejos para mejorar baloncesto|curiosidades baloncesto|momentos historicos nba|datos locos baloncesto)",
        "#futbol (historias increibles futbol|datos insolitos futbol|momentos historicos futbol|goles legendarios|anecdotas futbol|curiosidades jugadores futbol|analisis tactico futbol)",
        "#ciencia (biologia curiosidades| biologia desde 0|curiosidades del universo|fisica cuantica facil|experimentos increibles|datos cientificos sorprendentes|astronomia datos|animales increibles|flora fauna curiosidades|psicologia datos)",
        "#viajes (probando comida tradicional paises|lugares mas ocultos del mundo|mini vlog aventuras|mejores comidas del mundo|lugares sorprendentes|monumentos curiosidades|destinos increibles)",
        "#cine (detalles ocultos peliculas|analisis cinematografico|explicacion finales peliculas|explicacion series|critica cine|errores continuidad peliculas|curiosidades rodaje peliculas)",
        "#anime (detalles ocultos anime|sabias que anime|analisis anime|mejores anime temporada|estrenos anime|curiosidades anime|teorias anime|momentos epicos anime)",
        "#tecnologia (inventos tecnologicos futuro|trucos programacion ocultos|gadgets increibles|mejores telefonos android|consejos tecnologia|apps utiles desconocidas|historia tecnologia)",
        "#reflexion (consejos de vida practica|mejora personal|analisis personajes|crecimiento personal|motivacion real|reflexiones soledad|autocuidado tips|habitos personas exitosas)",
        "#autos (historias carreras legendarias|mecanica superdeportivos|restauraciones increibles autos|cultura jdm|records velocidad autos|curiosidades marcas autos)",
        "#curiosidades (datos que te volaran la cabeza|historias que pocos saben|hoteles mas raros del mundo|lugares 0 estrellas|sabias que datos|misterios sin resolver|hechos perturbadores historia)",
        "#comida (recetas faciles rapidas|reviews comida rapida|probando comida saludable|hamburguesas gigantes|comida fit tips|tips cocina casera|platos tipicos curiosidades)",
        "#historia (personajes historicos olvidados|civilizaciones antiguas curiosidades|misterios historia|datos locos historia|imperios caidos curiosidades|guerras curiosidades datos)",
        "#naturaleza (animales comportamiento increible|fauna submarina curiosidades|fenomenos naturales impresionantes|animales raros mundo|hechos sorprendentes naturaleza)",
        "#salud mental (ansiedad tecnicas relajacion|como dejar de procrastinar|motivacion real no toxica|terapia psicologica mitos|autoestima ejercicios practicos|gestion del estres laboral|mindfulness ejercicios diarios|como superar la soledad|depresion señales ayuda)",
        "#fitness y salud (ejercicios sin equipo casa|perder grasa habitos reales|comida saludable economica|rutina 10 min diaria|como ganar masa muscular|estirantes esenciales|mitos fitness aclarados|suplementos que funcionan)",
        "#mascotas (perros razas curiosidades|gatos comportamiento explicado|mascotas exoticas cuidados|adiestramiento canino tips|animales rescatados historias|veterinario consejos utiles|productos innovadores mascotas)",
        "#programacion y desarrollo (python trucos utiles|github proyectos interesantes|como aprender programacion gratis|errores comunes codigo|inteligencia artificial facil|usos creativos ia|desarrollo web consejos|tips)",
        "#diseño y branding (secretos ocultos en logos|por que las marcas usan ciertos colores|rediseños de marcas famosos|publicidad mas ingeniosa de la historia|errores millonarios de diseño|evolucion de marcas iconicas)",
        "#espacio y cosmos (que pasa si caes en un agujero negro|planetas mas extraños del universo|la vida en la estacion espacial|mensajes que enviamos al espacio|misiones espaciales que salieron mal|como busca la nasa vida extraterrestre)")


    // Bloques de entretenimiento (EN)
    private val ENTERTAINMENT_BLOCKS_EN = listOf(
        "#gaming (hidden secrets videogames|unusual game details|easter eggs curiosities|indie game analysis|videogame iceberg|top videogames|hidden indie games|famous videogame secrets)",
        "#basketball (incredible basketball plays|best basketball moves|tips to improve basketball|basketball curiosities|historic nba moments|crazy basketball facts)",
        "#football (incredible football stories|unusual football facts|historic football moments|legendary goals|football anecdotes|player curiosities|tactical football analysis)",
        "#science (biology curiosities|biology from scratch|universe curiosities|quantum physics explained easy|incredible experiments|surprising scientific facts|astronomy facts|incredible animals|flora fauna curiosities|psychology facts)",
        "#travel (trying traditional food from countries|most hidden places in the world|adventure mini vlog|best foods in the world|surprising places|monument curiosities|incredible destinations)",
        "#cinema (hidden details in movies|cinematographic analysis|movie ending explanations|series explanations|film criticism|movie continuity errors|film set curiosities)",
        "#anime (hidden anime details|did you know anime|anime analysis|best anime this season|anime premieres|anime curiosities|anime theories|epic anime moments)",
        "#technology (future tech inventions|hidden programming tricks|incredible gadgets|best android phones|technology tips|unknown useful apps|technology history)",
        "#selfgrowth (practical life advice|personal improvement|character analysis|personal growth|real motivation|solitude reflections|self care tips|successful people habits)",
        "#cars (legendary racing stories|supercar mechanics|incredible car restorations|jdm culture|car speed records|car brand curiosities)",
        "#curiosities (facts that will blow your mind|stories few people know|weirdest hotels in the world|zero star places|did you know facts|unsolved mysteries|disturbing history facts)",
        "#food (easy quick recipes|fast food reviews|trying healthy food|giant burgers|fit food tips|home cooking tips|typical dish curiosities)",
        "#history (forgotten historical figures|ancient civilization curiosities|history mysteries|crazy history facts|fallen empires curiosities|war curiosities facts)",
        "#nature (incredible animal behavior|underwater fauna curiosities|impressive natural phenomena|rarest animals in the world|surprising nature facts)",
        "#mental health (anxiety relaxation techniques|how to stop procrastinating|real non toxic motivation|therapy myths|self esteem practical exercises|work stress management|daily mindfulness exercises|how to overcome loneliness|depression warning signs help)",
        "#fitness and health (home no equipment exercises|real fat loss habits|affordable healthy food|10 min daily routine|how to gain muscle mass|essential stretches|fitness myths debunked|supplements that work)",
        "#pets (dog breed curiosities|cat behavior explained|exotic pet care|dog training tips|rescued animal stories|vet useful advice|innovative pet products)",
        "#coding and development (python useful tricks|interesting github projects|how to learn programming free|common code errors|artificial intelligence explained easy|creative ai uses|web development tips)",
        "#design and branding (hidden secrets in logos|why brands use certain colors|famous brand redesigns|most clever advertising in history|million dollar design mistakes|evolution of iconic brands)",
        "#space and cosmos (what happens if you fall into a black hole|strangest planets in the universe|life on the space station|messages we sent to space|space missions that went wrong|how nasa searches for extraterrestrial life)"
    )

    // Switch de bloques por region
    fun getEntertainmentBlocks(): List<String> {
        val lang = java.util.Locale.getDefault().language
        return if (lang == "es") ENTERTAINMENT_BLOCKS_ES else ENTERTAINMENT_BLOCKS_EN
    }
}