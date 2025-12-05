# inspect-core
[![Maven Central](https://img.shields.io/maven-central/v/io.github.oneteme/inspect-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.oneteme%22%20AND%20a:%22inspect-core%22)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/21-relnote-issues.html)

INSPECT — Évaluateur de Performance Système Intégré & Suivi de Communication.

`inspect-core` est une bibliothèque Java open-source conçue pour superviser et tracer le comportement des applications (monolithiques ou distribuées). Elle collecte des événements structurés sur l'activité de l'application (sessions et requêtes) — tels que le démarrage, l'exécution de batchs, le trafic HTTP entrant/sortant, les appels à des ressources externes et le traitement parallèle — et les transmet à un `inspect-server` central pour corrélation et analyse.

## Pourquoi INSPECT ?
- Obtenez une vue corrélée de bout en bout du traitement et des communications à travers votre système d'information.
- Tracez le trafic entrant et sortant, les exécutions de batchs et les événements de démarrage d'application de manière structurée et consultable.
- Suivez les traitements asynchrones et multi-threadés et corrélez les événements liés dans la même session.
- Centralisez l'analyse et l'agrégation des traces sur un `inspect-server` dédié pour détecter la latence, les erreurs et les problèmes de ressources.

## 1. Présentation
---------------
`inspect-core` capture deux concepts principaux :
- **Session** : représente une unité de traitement logique (par exemple : démarrage de l'application, un traitement par lots, la gestion d'une requête HTTP entrante, une exécution de test). Les sessions regroupent des événements connexes et permettent la corrélation des actions effectuées au cours de leur cycle de vie.
- **Requête** : représente une interaction avec une ressource externe ou locale (par exemple : HTTP, FTP/SFTP, LDAP/JNDI, SMTP, appels à la base de données ou opérations locales comme l'accès au cache et aux fichiers). Les requêtes incluent des métadonnées telles que la durée, le statut, le point de terminaison, la taille des données et les erreurs éventuelles.

## 2. Intégration
----------------------
Ajoutez la bibliothèque à votre projet :

**Maven**:
```xml
<dependency>
  <groupId>io.github.oneteme</groupId>
  <artifactId>inspect-core</artifactId>
  <version>1.1.17</version>
</dependency>
```

**Gradle**:
```groovy
implementation 'io.github.oneteme:inspect-core:1.1.17'
```

## 3. Configuration (exemple YAML)
-------------------------------
Ajoutez et adaptez ceci à votre `application.yml`:

```yaml
inspect:
  collector:
    enabled: true                # default=false, active le collecteur
    debug-mode: false            # default=false, active le mode de débogage pour le collecteur
    scheduling:
      interval: 5s               # default=60s, intervalle entre les envois de traces
    monitoring:
      http-route:
        excludes:
          method: OPTIONS        # default=[], exclut des méthodes HTTP spécifiques
          path: /favicon.ico, /actuator/info  # default=[], exclut les chemins correspondants
      resources:
        enabled: true            # collecte les métriques de base (mémoire / disque)
      exception:
        max-stack-trace-rows: -1 # -1 = illimité, limite les lignes de stack trace dans les traces
        max-cause-depth: -1      # -1 = illimité, limite la profondeur des causes imbriquées
    tracing:
      queue-capacity: 100        # default=10000, capacité de la file d'attente d'envoi des traces
      delay-if-pending: 0        # default=30, délai (s) si l'envoi précédent est en attente
      dump:
        enabled: true            # écrit les traces sur le disque local (utile pour le débogage)
      remote:
        mode: REST               # mode d'envoi (ex: REST)
        host: http://localhost:9001
        retention-max-age: 10d   # default=30d, période de rétention pour les traces locales
```

**Notes:**
- Ajustez `scheduling.interval` et `tracing.queue-capacity` en fonction de votre charge.
- Utilisez HTTPS pour `remote.host` en production pour sécuriser le transfert des traces.

## 4. Supervision — ce que `inspect-core` surveille
---------------------------------------------
- **Sessions**
  - **Démarrage** : capture les séquences de démarrage de l'application et les événements d'initialisation.
  - **Batch** : suit les exécutions de jobs et de traitements par lots (durée, succès/échec, exceptions).
  - **HTTP Entrant** : suit le traitement des requêtes HTTP entrantes (méthode, chemin, statut, temps).
  - **Tests** : capture les exécutions de tests pour la corrélation dans les environnements d'intégration.

- **Requêtes**
  - **HTTP** : appels HTTP sortants et entrants (URL, méthode, en-têtes, taille des données, statut de la réponse).
  - **FTP / SFTP** : métadonnées de connexion et de transfert (hôte, port, utilisateur, durée, erreurs).
  - **LDAP / JNDI** : opérations de recherche/liste/attributs (point de terminaison, utilisateur, durée).
  - **SMTP** : opérations d'envoi d'e-mails (métadonnées du message, destinataires, taille, statut de livraison).
  - **Opérations Locales** : accès au cache, opérations sur les fichiers, opérations constantes ou en mémoire (contexte pour le travail non lié au réseau).

- **Threads**
  - Suit et corrèle le travail exécuté sur d'autres threads afin que le traitement parallèle ou asynchrone soit inclus dans la session parente et non perdu.

- **Ressources**
  - Métriques de base de la machine telles que l'utilisation de la mémoire et du disque, collectées périodiquement pour corréler la consommation de ressources avec l'activité de l'application.

## 5. Sécurité & Confidentialité
---------------------
- Les traces peuvent inclure des en-têtes et des métadonnées de charge utile. Soyez prudent avec les données sensibles (en-têtes d'autorisation, données personnelles, secrets).
- Préférez le filtrage ou le masquage des champs sensibles avant l'envoi des traces.
- Sécurisez le transport vers `inspect-server` (HTTPS) et contrôlez les politiques d'accès/rétention côté serveur.

## 6. Bonnes pratiques
-----------------
- **Configuration Prudente** : N'activez que les moniteurs dont vous avez besoin pour éviter une surcharge de performance.
- **Gestion des Données Sensibles** : Utilisez les fonctionnalités de masquage ou de filtrage pour éviter de tracer des informations sensibles.
- **Dimensionnement** : Ajustez la capacité de la file d'attente et l'intervalle d'envoi en fonction de la charge de votre application pour éviter la perte de traces.
- **Alertes** : Configurez des alertes sur votre `inspect-server` pour être notifié proactivement des erreurs ou des problèmes de performance.
- **Tests** : Intégrez le suivi dans vos tests d'intégration pour identifier les régressions de performance avant la mise en production.

## 7. Contribuer
-----------------
Les contributions sont les bienvenues ! Veuillez consulter `CODE_OF_CONDUCT.md` et soumettre une pull request.

## 8. Licence
----------
Ce projet est sous licence Apache 2.0. Voir le fichier [LICENSE](LICENSE) pour plus de détails.
- Enable `debug-mode` and `dump` only for diagnostics; both increase I/O and logs.
- Configure sensible defaults for `queue-capacity` and `scheduling.interval` based on expected traffic.
- Exclude non-relevant routes (OPTIONS, favicon, health checks) to reduce noise.
- Apply data redaction where required to comply with privacy regulations.

7. Quickstart
-------------
1. Add the dependency to your project.
2. Add the example configuration in your `application.yml` and set `collector.enabled: true`.
3. Configure `inspect.tracing.remote.host` to point to your inspect-server (or enable local dump).
4. Start the application and verify traces are received by your inspect-server or written to disk.

8. Tests, build & contribution
------------------------------
- Build and test with Maven:
  ```bash
  mvn clean test
  mvn -DskipTests package
  ```
- Contribution model: fork → branch → pull request. Add tests and documentation for new features or fixes.
- Respect the repository Code of Conduct.

9. License & contact
--------------------
- License: Apache License 2.0 (see LICENSE file).
- Report issues or request features via GitHub issues on this repository.

---

```
## Collectors

| Request  | CLASS        |
|----------|--------------|
| JDBC     | javax.sql.DataSource |
| LDAP     | javax.naming.directory.DirContext |
| SMTP     | jakarta.mail.Transport |
| HTTP     | org.springframework.http.client.ClientHttpRequestInterceptor <br> org.springframework.web.reactive.function.client.ExchangeFilterFunction |
| FTP      | com.jcraft.jsch.ChannelSftp |
| LOCAL    | org.aspectj.lang.annotation.Aspect |



