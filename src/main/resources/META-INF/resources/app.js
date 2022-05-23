import * as pages from './pages/index.js'
import homepage from './pages/home.js'
import store from './store.js'

export default {
    name: 'App',
    components: Object.assign({homepage}, pages),

    data() {
        return {
            logCriticalErrors: []
        }
    },
    setup() {
        const {watchEffect, onMounted, ref} = Vue;
        const page = ref(null);

        //store management: save $variables to localstorage
        onMounted(() => {
            window.addEventListener('beforeunload', () => {
                Object.keys(store).forEach(function (key) {
                    if (key.charAt(0) == "$") {
                        localStorage.setItem(key, store[key]);
                    } else {
                        localStorage.removeItem("$" + key);
                    }
                });
            });
            Object.keys(store).forEach(function (key) {
                    if (key.charAt(0) == "$") {
                        if (localStorage.getItem(key)) store[key] = localStorage.getItem(key);
                    }
                }
            )
        })
        //url management
        watchEffect(() => {
            const urlpage = window.location.pathname.split("/").pop();
            if (page.value == null) {
                page.value = urlpage
            }
            if (page.value != urlpage) {
                const url = page.value ? page.value : './';
                window.history.pushState({url: url}, '', url);
            }
            window.onpopstate = function () {
                page.value = window.location.pathname.split("/").pop()
            };
        });

        const eventBus = new EventBus('/eventbus');

        eventBus.onopen = () => {
            eventBus.registerHandler('LOG_ERR_CRITICAL', (error, message) => {
                let div = document.createElement("div")
                div.append(message.body)
                document.getElementById("critical_error").append(div);
            })
        }
        return {
            page, pages, eventBus
        };
    },
    template: `
        <div id="sidebar">
            <nav class="nav">
                    <h5 class="nav-logo" v-on:click="page = ''" >
                         Tinkoff Spread Robot
                    </h5>
                    <template v-for="item, index in pages" key="item.name">
                        <a v-on:click="page = index" class="nav-item">
                            {{ item.name }}
                        </a>
                    </template>
            </nav>
       </div>
        <div id="content">
            <div id="critical_error" class="log-critical-error-panel"></div>
            <component :is="page || 'homepage'"></component>
        </div>
    `,
};