# Sprint 9 — Sessions HTTP et registre applicatif

Pour chaque route, `FrontServlet` appelle `request.getSession(true)`. Le conteneur
retourne la session HTTP existante pour ce client ou en crée une si nécessaire.
`SessionRegistryListener.getCurrentSession(request)` regroupe ces étapes. Un contrôleur
peut recevoir `MySession` comme paramètre de méthode.

`SessionRegistryListener` crée une `ConcurrentHashMap<String, MySession>` dans le
`ServletContext`, sous la clé `framework.activeSessions`. `MySession` enveloppe la
`HttpSession` fournie par le conteneur et expose les opérations usuelles ainsi que
`setUser(Object)` / `getUser(Class<T>)`. Le type `User` appartient à l'application et le
framework ne doit donc pas dépendre d'une classe `User` qui n'existe pas ici.

Le listener ajoute les sessions à leur création, les retire à leur destruction et met à
jour la clé si l'identifiant de session change. La clé est l'identifiant de session du
conteneur :

```java
Map<String, MySession> sessions = SessionRegistryListener.getActiveSessions(getServletContext());
```

La map est partagée par toute l'application web. Une `HttpSession` appartient à une session
client, pas nécessairement à une personne : une même personne peut avoir des sessions
différentes dans plusieurs navigateurs ou appareils. Ce registre suit donc les sessions
HTTP actives, pas les utilisateurs authentifiés. Pour indexer réellement par utilisateur,
il faut authentifier l'utilisateur, choisir un identifiant utilisateur stable, puis décider
comment gérer ses connexions simultanées. Le `ServletContext` est partagé par l'application
web ; ce n'est pas lui qui crée les sessions HTTP, le conteneur les crée via
`request.getSession(true)`.

`@WebListener` déclare le listener auprès du conteneur. L'API Jakarta Servlet prévoit
`HttpSessionListener` pour recevoir les événements de création et de destruction des
sessions.
